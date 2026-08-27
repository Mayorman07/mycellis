package com.mycelis.shared.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user rate limit on stalk creation only: 5/minute and 25/day, enforced
 * by one Bucket4j bucket with two stacked bandwidths. Bucket4j treats
 * multi-bandwidth consumption as all-or-nothing internally — both limits
 * must have room or neither is consumed, so no manual probe-combining or
 * rollback logic is needed here.
 *
 * <p>Runs after {@code SecurityContextHolderFilter} (see SecurityConfig) so
 * the authenticated principal is already available. Writes its own 429
 * response directly rather than throwing, since filters run outside Spring
 * MVC's {@code @ExceptionHandler} machinery.</p>
 *
 * <p>TODO: in-memory buckets become per-instance if we scale past one Fly
 * machine. Revisit with Upstash Redis if we go horizontal.</p>
 */
@Slf4j
@Component
public class Bucket4jRateLimitFilter extends OncePerRequestFilter {

    private static final String BASE_URI = "https://mycellis.dev/errors/";
    private static final String TARGET_PATH = "/api/stalks";

    private final ConcurrentHashMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    // Not injected: no ObjectMapper bean is registered in this app context
    // (see IntegrationTestBase's own note on the same gap). This filter's
    // only use is serializing one ProblemDetail per rejected request, so a
    // private instance is simplest — no need to reopen that gap app-wide
    // just for this.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod()) || !TARGET_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof MycelisUserPrincipal principal)) {
            // No authenticated principal yet — let it through. The normal
            // authorization chain (.anyRequest().authenticated()) rejects it
            // with 401 downstream; there's nothing to rate-limit for a caller
            // who isn't logged in.
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(principal.getId(), id -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
        log.warn("Rate limit exceeded: userId={}, retryAfterSeconds={}", principal.getId(), retryAfterSeconds);
        writeRateLimitResponse(response, retryAfterSeconds);
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .addLimit(Bandwidth.classic(25, Refill.intervally(25, Duration.ofDays(1))))
                .build();
    }

    private void writeRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "You've created stalks too quickly.");
        pd.setTitle("Rate Limit Exceeded");
        pd.setType(URI.create(BASE_URI + "rate-limit-exceeded"));
        pd.setProperty("timestamp", Instant.now());

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(pd));
    }
}
