package com.mycelis.shared.config;

import com.mycelis.shared.ratelimit.Bucket4jRateLimitFilter;
import com.mycelis.user.security.MycelisUserDetailsService;
import com.mycelis.user.security.RestAccessDeniedHandler;
import com.mycelis.user.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MycelisUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final Bucket4jRateLimitFilter bucket4jRateLimitFilter;

    // Cloudflare Pages (mycellis.dev) and Fly.io (api.mycellis.dev) are
    // separate origins in production — without this, every authenticated
    // cross-origin request fails the browser's CORS preflight. The
    // :http://localhost:5173 default keeps dev working unchanged and means
    // a missing CORS_ALLOWED_ORIGINS on Fly degrades to a safe default
    // instead of crashing boot.
    @Value("${mycelis.cors.allowed-origins:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> allowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // required for session cookies
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    /**
     * Suppresses Spring Boot's default servlet-filter auto-registration for
     * Bucket4jRateLimitFilter. Do NOT delete this thinking it's dead code —
     * removing it silently disables rate limiting again.
     *
     * <p>Bucket4jRateLimitFilter is a {@code @Component} implementing
     * {@code Filter}, and it's ALSO manually wired into the security chain
     * below via {@code .addFilterAfter(bucket4jRateLimitFilter,
     * CorsFilter.class)}. That combination is a well-known
     * Spring Boot + Spring Security footgun: because the filter is a plain
     * bean implementing {@code Filter}, Spring Boot's own servlet filter
     * auto-configuration ALSO registers it generically in the
     * ApplicationContext-driven filter chain — entirely independently of,
     * and with no awareness of, its manual position inside
     * {@code HttpSecurity}'s chain. That generic registration can execute at
     * a point in the overall pipeline where Spring Security hasn't yet
     * restored the session's {@code Authentication}, so the filter's
     * principal check fails there and it passes the request straight
     * through. The problem isn't that this happens once — it's that
     * {@code OncePerRequestFilter} guarantees its real logic (
     * {@code doFilterInternal}) only ever runs once per request, tracked via
     * a request attribute, regardless of which registration triggered the
     * first invocation. So that first, wrongly-positioned, no-principal
     * pass-through then silently suppresses the SECOND, correctly-positioned
     * invocation inside {@code HttpSecurity}'s own chain — the one that
     * would have actually seen the authenticated principal and enforced the
     * rate limit. Net effect: the filter runs, does nothing, every single
     * time, with no exceptions anywhere to signal that anything is wrong.
     * This is exactly what happened in production after PR #5 shipped
     * without this bean: a user created 11 stalks in ~2 minutes with zero
     * 429 responses, against a configured limit of 5/minute. Neither the
     * filter's own unit test nor the MockMvc integration test caught this,
     * because neither test path goes through Boot's real generic
     * servlet-filter auto-registration/ordering machinery the way a live
     * deployment does — both passed throughout the entire time this bug was
     * live in production.</p>
     *
     * <p>{@code setEnabled(false)} leaves {@code HttpSecurity}'s manual
     * {@code .addFilterAfter(...)} wiring below as the ONLY place this
     * filter is ever registered.</p>
     */
    @Bean
    public FilterRegistrationBean<Bucket4jRateLimitFilter> bucket4jRateLimitFilterRegistration(
            Bucket4jRateLimitFilter filter) {
        FilterRegistrationBean<Bucket4jRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Suppress auto-registration; filter is manually wired via HttpSecurity.addFilterAfter
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider) throws Exception { {

        http
                // CSRF: cookie-based token for session SPAs.
                // withHttpOnlyFalse so JS can read it and echo it in the X-XSRF-TOKEN header.
                .csrf(AbstractHttpConfigurer::disable)

                // Cross-origin requests (mycellis.dev -> api.mycellis.dev in prod)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Persist SecurityContext to HTTP session so login is "sticky"
                // across requests via the JSESSIONID cookie.
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository())
                )

                // Stateful sessions for the dashboard
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(3) // up to 3 concurrent logins per user
                )

                // Route rules
                .authorizeHttpRequests(auth -> auth
                        // Public API endpoints
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/logout",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify",
                                "/api/auth/resend-verification",
                                "/api/users/create"
                        ).permitAll()

                        // Public status pages — no login required, visited by anyone with the link
                        .requestMatchers("/api/status/**").permitAll()

                        // Public static pages (frontend lives at root)
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/verify",
                                "/verify.html",
                                "/login",
                                "/login.html",
                                "/resend-verification",
                                "/resend-verification.html",
                                "/reset-password",
                                "/reset-password.html",
                                "/forgot-password",
                                "/forgot-password.html",
                                "/favicon.ico"
                        ).permitAll()

                        // Static assets (CSS/JS/images served from /static/)
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/assets/**",
                                "/fonts/**"
                        ).permitAll()

                        // Health/metrics — "/actuator/health/**" (not just the exact path)
                        // covers the liveness/readiness health groups Fly's health check
                        // and probes hit as sub-paths (e.g. /actuator/health/readiness),
                        // which an exact-string matcher would otherwise send to
                        // .anyRequest().authenticated() and always 401.
                        .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info").permitAll()

                        // OpenAPI / Swagger UI in dev
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Everything else needs auth
                        .anyRequest().authenticated()
                )

                // JSON error responses instead of HTML redirects
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                // Per-user rate limit on POST /api/stalks only. Anchored after
                // CorsFilter so that (a) the authenticated principal is available
                // transitively (CorsFilter runs after SecurityContextHolderFilter),
                // AND (b) CorsFilter and HeaderWriterFilter have set their response
                // headers before this filter runs — critical because this filter
                // short-circuits on rejection without invoking filterChain.doFilter(),
                // and neither CorsFilter nor HeaderWriterFilter have post-processing
                // legs. Without this anchor, 429 responses would ship without CORS
                // or security headers, and browsers block credentialed responses
                // without proper CORS. See Bucket4jRateLimitFilter's class javadoc
                // for the full history.
                .addFilterAfter(bucket4jRateLimitFilter, CorsFilter.class)

                //  disable Spring's default login page
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authenticationProvider(authenticationProvider);

        return http.build();
    }
  }
}