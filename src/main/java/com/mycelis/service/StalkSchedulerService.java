package com.mycelis.service;

import com.mycelis.config.MonitoringProperties;
import com.mycelis.entity.Stalk;
import com.mycelis.engine.PulseEngine;
import com.mycelis.repository.StalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * Fast-tick scheduler that processes capped batches of due monitoring targets.
 * Uses SpEL + Duration for type-safe, ISO-8601 compliant configuration.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Fixed-delay ticks with Duration config → predictable intervals with drift compensation</li>
 *   <li>Batch capping via Pageable → backpressure control and smooth DB load</li>
 *   <li>Jittered rescheduling → prevents thundering herd on shared intervals</li>
 *   <li>Graceful error isolation → single failures don't halt the cycle</li>
 *   <li>Type-safe time config via Duration → zero unit confusion, ISO-8601 compliant</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StalkSchedulerService {

    private final StalkRepository stalkRepository;
    private final PulseEngine pulseEngine;
    private final MonitoringProperties monitoringProperties;

    // Thread-local random for jitter calculation (avoids contention)
    private static final ThreadLocal<Random> jitterRandom = ThreadLocal.withInitial(Random::new);

    /**
     * Fast-tick scheduler: runs every N milliseconds, processes capped batches.
     * Uses SpEL to bind Duration config to fixedDelay attribute.
     *
     * <p>SpEL expression breakdown:
     * <ul>
     *   <li>{@code #{...}} = Spring Expression Language evaluation</li>
     *   <li>{@code @monitoringProperties} = reference to the Spring bean</li>
     *   <li>{@code .schedulerTickInterval} = getter for Duration field</li>
     *   <li>{@code .toMillis()} = converts Duration to long for @Scheduled</li>
     * </ul>
     * </p>
     */
    @Scheduled(fixedDelayString = "#{@monitoringProperties.schedulerTickInterval.toMillis()}")
    @Transactional
    public void runCheckCycle() {
        Instant cycleStart = Instant.now();
        log.debug("Scheduler tick started");

        try {
            // 1. Fetch due stalks with pessimistic lock + batch cap
            var batchPage = org.springframework.data.domain.PageRequest.of(0, monitoringProperties.getMaxBatchSize());
            List<Stalk> dueStalks = stalkRepository.findDueForCheck(cycleStart, batchPage);

            if (dueStalks.isEmpty()) {
                log.trace("No stalks due in this tick");
                return;
            }

            log.info("Processing batch of {} stalks (max: {})", dueStalks.size(), monitoringProperties.getMaxBatchSize());

            // 2. Dispatch to engine (virtual threads handle concurrency)
            pulseEngine.executeCycle(dueStalks);

            // 3. Update next_check_at with jitter
            int updated = updateNextCheckTimes(dueStalks, Instant.now());

            Duration elapsed = Duration.between(cycleStart, Instant.now());
            log.info("Tick completed: elapsed={}, processed={}, updated={}",
                    elapsed, dueStalks.size(), updated);

        } catch (Exception e) {
            log.error("Scheduler tick failed", e);
            // Swallow to prevent scheduler thread termination
        }
    }

    /**
     * Updates next_check_at for processed stalks with jitter to prevent thundering herd.
     *
     * @param stalks list of stalks that were just checked
     * @param now current timestamp
     * @return count of successfully updated stalks
     */
    private int updateNextCheckTimes(List<Stalk> stalks, Instant now) {
        int updated = 0;

        for (Stalk stalk : stalks) {
            try {
                int baseInterval = stalk.getGrowthIntervalSeconds();
                int jitter = calculateJitter(baseInterval);
                Instant nextCheck = now.plusSeconds(baseInterval + jitter);

                stalkRepository.updateNextCheckAt(stalk.getId(), nextCheck, now);
                updated++;

                log.trace("Updated next_check_at for stalk {}: {} (jitter: {}s)",
                        stalk.getId(), nextCheck, jitter);

            } catch (Exception e) {
                // Log but don't fail the entire cycle for a single stalk update failure
                log.warn("Failed to update next_check_at for stalk {}: {}",
                        stalk.getId(), e.getMessage());
            }
        }
        return updated;
    }

    /**
     * Calculates a random jitter value to spread out scheduled checks.
     * Prevents thundering herd when many stalks share the same interval.
     *
     * @param baseIntervalSeconds the configured check interval
     * @return jitter value in seconds (±jitterPercent% of base interval)
     */
    private int calculateJitter(int baseIntervalSeconds) {
        int jitterPercent = monitoringProperties.getSchedulerJitterPercent();
        int range = (int) (baseIntervalSeconds * (jitterPercent / 100.0));
        return jitterRandom.get().nextInt(range * 2 + 1) - range;
    }

    /**
     * Optional: Manual trigger for testing or emergency re-checks.
     * Not exposed via API in production (add @PreAuthorize if needed).
     */
    @Transactional
    public void triggerManualCheck(List<Stalk> stalks) {
        if (stalks == null || stalks.isEmpty()) {
            log.debug("Manual check triggered with empty stalk list");
            return;
        }

        log.info("Manual check triggered for {} stalks", stalks.size());
        pulseEngine.executeCycle(stalks);
        updateNextCheckTimes(stalks, Instant.now());
    }
}