package com.mycelis.monitoring.service;

import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.shared.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Handles the transactional "claim" phase of the scheduler tick.
 *
 * <p>Lives in its own Spring bean (separate from StalkSchedulerService) so that
 * @Transactional is honored through the Spring proxy. Self-invocation within
 * StalkSchedulerService would bypass the proxy and silently lose the transaction.</p>
 *
 * <p>This guarantees: SELECT FOR UPDATE → rescheduling Updates → COMMIT, all completed
 * before control returns to the caller. Row locks are released before the HTTP work
 * in PulseEngine begins.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StalkClaimService {

    private final StalkRepository stalkRepository;
    private final MonitoringProperties monitoringProperties;

    private static final ThreadLocal<Random> jitterRandom = ThreadLocal.withInitial(Random::new);

    /**
     * Atomically claims due stalks and reschedules them.
     * Transaction commits when this method returns, releasing row locks.
     */
    @Transactional(timeout = 5)  // seconds; fail fast if DB is unhealthy
    public List<Stalk> claimDueStalks(Instant now, int limit) {
        List<Stalk> dueStalks = stalkRepository.findDueForCheck(now, limit);

        if (dueStalks.isEmpty()) {
            return Collections.emptyList();
        }

        for (Stalk stalk : dueStalks) {
            int baseInterval = stalk.getGrowthIntervalSeconds();
            int jitter = calculateJitter(baseInterval);
            Instant nextCheck = now.plusSeconds(baseInterval + jitter);

            stalkRepository.rescheduleAfterCheck(stalk.getId(), nextCheck, now);
        }

        return dueStalks;
    }

    private int calculateJitter(int baseIntervalSeconds) {
        int jitterPercent = monitoringProperties.getSchedulerJitterPercent();
        int range = (int) (baseIntervalSeconds * (jitterPercent / 100.0));
        return jitterRandom.get().nextInt(range * 2 + 1) - range;
    }
}