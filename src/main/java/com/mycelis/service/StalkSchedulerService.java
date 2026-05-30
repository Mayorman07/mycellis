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

    @Scheduled(fixedDelayString = "#{@monitoringProperties.schedulerTickInterval.toMillis()}")
    @Transactional
    public void runCheckCycle() {
        Timer.Sample sample = Timer.start(meterRegistry);
        Instant cycleStart = Instant.now();
        log.debug("Scheduler tick started");

        try {
            // 1. Fetch due stalks with SKIP LOCKED + batch cap
            List<Stalk> dueStalks = stalkRepository.findDueForCheck(
                    cycleStart,
                    monitoringProperties.getMaxBatchSize()
            );

            if (dueStalks.isEmpty()) {
                log.trace("No stalks due in this tick");
                tickEmptyCounter.increment();
                return;
            }

            log.info("Processing batch of {} stalks (max: {})", dueStalks.size(), monitoringProperties.getMaxBatchSize());

            // 2. Dispatch to engine (virtual threads handle concurrency)
            pulseEngine.executeCycle(dueStalks);

            // 3. Update next_check_at with jitter
            int updated = updateNextCheckTimes(dueStalks, Instant.now());

            // Record processed/updated metrics
            tickProcessedCounter.increment(dueStalks.size());
            tickUpdatedCounter.increment(updated);

            Duration elapsed = Duration.between(cycleStart, Instant.now());
            log.info("Tick completed: elapsed={}, processed={}, updated={}",
                    elapsed, dueStalks.size(), updated);

        } catch (Exception e) {
            log.error("Scheduler tick failed", e);
            tickFailedCounter.increment();
            // Swallow to prevent scheduler thread termination
        } finally {
            // Stop timer regardless of success/failure
            sample.stop(tickDurationTimer);
        }
    }

    private int updateNextCheckTimes(List<Stalk> stalks, Instant now) {
        int updated = 0;

        for (Stalk stalk : stalks) {
            try {
                int baseInterval = stalk.getGrowthIntervalSeconds();
                int jitter = calculateJitter(baseInterval);
                Instant nextCheck = now.plusSeconds(baseInterval + jitter);

                int rowsUpdated = stalkRepository.rescheduleAfterCheck(stalk.getId(), nextCheck, now);
                if (rowsUpdated == 1) {
                    updated++;
                    log.trace("Updated next_check_at for stalk {}: {} (jitter: {}s)",
                            stalk.getId(), nextCheck, jitter);
                } else {
                    log.warn("Expected 1 row updated for stalk {}, got {}", stalk.getId(), rowsUpdated);
                }

            } catch (Exception e) {
                // Log but don't fail the entire cycle for a single stalk update failure
                log.warn("Failed to update next_check_at for stalk {}: {}",
                        stalk.getId(), e.getMessage());
            }
        }
        return updated;
    }

    private int calculateJitter(int baseIntervalSeconds) {
        int jitterPercent = monitoringProperties.getSchedulerJitterPercent();
        int range = (int) (baseIntervalSeconds * (jitterPercent / 100.0));
        return jitterRandom.get().nextInt(range * 2 + 1) - range;
    }

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