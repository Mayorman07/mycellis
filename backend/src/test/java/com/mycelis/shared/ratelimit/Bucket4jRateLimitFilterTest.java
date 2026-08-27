package com.mycelis.shared.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the exact bandwidth configuration Bucket4jRateLimitFilter uses
 * (5/minute + 25/day, stacked on one bucket) directly via Bucket4j's own
 * API — no servlet/Spring plumbing needed to verify the throttling design
 * itself. The filter's HTTP-level behavior (principal extraction, 429
 * response shape, Retry-After header) is covered by the integration test in
 * CreateStalkIntegrationTest instead.
 */
class Bucket4jRateLimitFilterTest {

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
}
