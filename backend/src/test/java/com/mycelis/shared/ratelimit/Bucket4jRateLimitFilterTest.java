package com.mycelis.shared.ratelimit;

import com.mycelis.user.security.MycelisUserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Two things live in this file, deliberately: the throttling design itself
 * (bandwidth math, exercised directly via Bucket4j's own API — no servlet
 * plumbing needed for that part), AND the filter's actual HTTP-level
 * response shape (Content-Type/charset, status), exercised by invoking
 * Bucket4jRateLimitFilter.doFilterInternal directly with Spring's real
 * MockHttpServletResponse.
 *
 * <p>That second half didn't always exist here — this file originally
 * assumed the filter had "no HTTP behavior worth testing" and left that
 * entirely to CreateStalkIntegrationTest's slower, full-Spring-context
 * MockMvc test. That assumption was wrong: a missing charset on the 429
 * response (fixed alongside this test) shipped to prod undetected because
 * neither this file nor the integration test asserted on Content-Type at
 * the time. Don't remove the HTTP-level test below on the assumption that
 * the bucket-logic tests above are "the real test" — they cover a
 * genuinely different failure class.</p>
 */
class Bucket4jRateLimitFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .addLimit(Bandwidth.classic(25, Refill.intervally(25, Duration.ofDays(1))))
                .build();
    }

    @Test
    void allowsFirstFiveConsumesWithinAMinute() {
        Bucket bucket = newBucket();

        for (int i = 0; i < 5; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertThat(probe.isConsumed()).isTrue();
        }
    }

    @Test
    void rejectsSixthConsumeWithinTheSameMinute() {
        Bucket bucket = newBucket();

        for (int i = 0; i < 5; i++) {
            bucket.tryConsumeAndReturnRemaining(1);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        assertThat(probe.isConsumed()).isFalse();
    }

    @Test
    void nanosToWaitForRefillIsPositiveAndWithinTheMinuteWindow() {
        Bucket bucket = newBucket();

        for (int i = 0; i < 5; i++) {
            bucket.tryConsumeAndReturnRemaining(1);
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        assertThat(probe.getNanosToWaitForRefill()).isPositive();
        assertThat(probe.getNanosToWaitForRefill()).isLessThanOrEqualTo(Duration.ofMinutes(1).toNanos());
    }

    @Test
    void dailyBandwidthRejectsTheTwentySixthConsumeEvenAcrossSeparateMinutes() {
        // Simulates the daily cap being hit well before the per-minute window
        // would ever refill enough to allow it — 25 tokens consumed one at a
        // time is enough to prove the second bandwidth is enforced
        // independently of the first, without needing to fast-forward a clock.
        Bucket bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(1000, Refill.intervally(1000, Duration.ofMinutes(1))))
                .addLimit(Bandwidth.classic(25, Refill.intervally(25, Duration.ofDays(1))))
                .build();

        for (int i = 0; i < 25; i++) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            assertThat(probe.isConsumed()).isTrue();
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        assertThat(probe.isConsumed()).isFalse();
    }

    /**
     * Regression test for a real prod bug: the 429 response was missing an
     * explicit UTF-8 charset, so it fell back to the servlet container's
     * ISO-8859-1 default — browsers refused to parse it, and PR #5's live
     * countdown UI never appeared. MockHttpServletResponse (Spring's, not a
     * Mockito mock) is what makes this test meaningful: it genuinely models
     * servlet charset defaults, so it would have caught this exact bug
     * before it reached prod.
     */
    @Test
    void writesUtf8CharsetOnRateLimitedResponse() throws Exception {
        Bucket4jRateLimitFilter filter = new Bucket4jRateLimitFilter();
        MycelisUserPrincipal principal = new MycelisUserPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), List.of(), false,
                "ratelimit-test@example.test", "password", true, true, true, true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        FilterChain filterChain = mock(FilterChain.class);

        // Exhaust the per-minute bandwidth (5 tokens) first.
        for (int i = 0; i < 5; i++) {
            filter.doFilterInternal(
                    new MockHttpServletRequest("POST", "/api/stalks"), new MockHttpServletResponse(), filterChain);
        }

        // 6th request in the same window should be rejected.
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(new MockHttpServletRequest("POST", "/api/stalks"), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).isEqualTo("application/problem+json;charset=UTF-8");
    }
}
