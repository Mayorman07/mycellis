package com.mycelis.shared.config;

import com.mycelis.user.security.MycelisUserDetailsService;
import com.mycelis.user.security.RestAccessDeniedHandler;
import com.mycelis.user.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MycelisUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

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

                        // Health/metrics
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

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

                //  disable Spring's default login page
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authenticationProvider(authenticationProvider);

        return http.build();
    }
  }
}