package com.mycelis.service;

import com.mycelis.config.MonitoringProperties;
import com.mycelis.entity.Stalk;
import com.mycelis.engine.PulseEngine;
import com.mycelis.repository.StalkRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class StalkSchedulerService {

    private final StalkRepository stalkRepository;
    private final PulseEngine pulseEngine;
    private final MonitoringProperties monitoringProperties;
    private final MeterRegistry meterRegistry;

    // Metrics: Counters
    private final Counter tickProcessedCounter;
    private final Counter tickUpdatedCounter;
    private final Counter tickEmptyCounter;
    private final Counter tickFailedCounter;

    // Metrics: Timer for tick duration
    private final Timer tickDurationTimer;

    // Thread-local random for jitter calculation (avoids contention)
    private static final ThreadLocal<Random> jitterRandom = ThreadLocal.withInitial(Random::new);

    public StalkSchedulerService(StalkRepository stalkRepository,
                                 PulseEngine pulseEngine,
                                 MonitoringProperties monitoringProperties,
                                 MeterRegistry meterRegistry) {
        this.stalkRepository = stalkRepository;
        this.pulseEngine = pulseEngine;
        this.monitoringProperties = monitoringProperties;
        this.meterRegistry = meterRegistry;

        // Register counters
        this.tickProcessedCounter = Counter.builder("app.scheduler.tick.processed")
                .description("Number of stalks processed per scheduler tick")
                .register(meterRegistry);

        this.tickUpdatedCounter = Counter.builder("app.scheduler.tick.updated")
                .description("Number of stalks with updated next_check_at per tick")
                .register(meterRegistry);

        this.tickEmptyCounter = Counter.builder("app.scheduler.tick.empty")
                .description("Number of ticks with no due stalks")
                .register(meterRegistry);

        this.tickFailedCounter = Counter.builder("app.scheduler.tick.failed")
                .description("Number of failed scheduler ticks")
                .register(meterRegistry);

        // Register timer
        this.tickDurationTimer = Timer.builder("app.scheduler.tick.duration")
                .description("Duration of a complete scheduler tick cycle")
                .register(meterRegistry);
    }

    /**
     * Fast-tick scheduler: runs every N milliseconds, processes capped batches.
     * Transaction boundary is detached: claim work atomically, then dispatch non-transactionally.
     */
    @Scheduled(fixedDelayString = "#{@monitoringProperties.schedulerTickInterval.toMillis()}")
    public void runCheckCycle() {  // ← NO @Transactional here
        Timer.Sample sample = Timer.start(meterRegistry);
        Instant cycleStart = Instant.now();
        log.debug("Scheduler tick started");

        try {
            // Phase 1: CLAIM WORK (transactional, fast, releases locks immediately)
            List<Stalk> dueStalks = claimDueStalks(cycleStart, monitoringProperties.getMaxBatchSize());

            if (dueStalks.isEmpty()) {
                log.trace("No stalks due in this tick");
                tickEmptyCounter.increment();
                return;
            }

            log.info("Processing batch of {} stalks (max: {})", dueStalks.size(), monitoringProperties.getMaxBatchSize());

            // Phase 2: DISPATCH TO ENGINE (non-transactional, virtual threads publish events & die)
            pulseEngine.executeCycle(dueStalks);

            // Record metrics (fast, sync)
            tickProcessedCounter.increment(dueStalks.size());
            tickUpdatedCounter.increment(dueStalks.size());  // All were rescheduled in claim phase

            Duration elapsed = Duration.between(cycleStart, Instant.now());
            log.info("Tick completed: elapsed={}, processed={}, updated={}",
                    elapsed, dueStalks.size(), dueStalks.size());

        } catch (Exception e) {
            log.error("Scheduler tick failed", e);
            tickFailedCounter.increment();
            // Swallow to prevent scheduler thread termination
        } finally {
            sample.stop(tickDurationTimer);
        }
    }

    /**
     * Claims due stalks and reschedules them atomically.
     * Transaction commits immediately after this method returns, releasing row locks.
     * This allows virtual threads to write to pulses table without blocking on parent stalk locks.
     */
    @Transactional
    public List<Stalk> claimDueStalks(Instant now, int limit) {
        List<Stalk> dueStalks = stalkRepository.findDueForCheck(now, limit);

        if (dueStalks.isEmpty()) {
            return Collections.emptyList();
        }

        // Reschedule IMMEDIATELY (still in same transaction)
        for (Stalk stalk : dueStalks) {
            int baseInterval = stalk.getGrowthIntervalSeconds();
            int jitter = calculateJitter(baseInterval);
            Instant nextCheck = now.plusSeconds(baseInterval + jitter);

            // Update next_check_at + last_checked_at + updatedAt in one atomic operation
            stalkRepository.rescheduleAfterCheck(stalk.getId(), nextCheck, now);
        }

        //  Transaction commits HERE → locks released → virtual threads can proceed with FK validation
        return dueStalks;
    }

    /**
     * Calculates a random jitter value to spread out scheduled checks.
     * Prevents thundering herd when many stalks share the same interval.
     */
    private int calculateJitter(int baseIntervalSeconds) {
        int jitterPercent = monitoringProperties.getSchedulerJitterPercent();
        int range = (int) (baseIntervalSeconds * (jitterPercent / 100.0));
        return jitterRandom.get().nextInt(range * 2 + 1) - range;
    }

    /**
     * Manual trigger for testing or emergency re-checks.
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
        // Note: Manual checks don't reschedule - they're one-off
    }
}