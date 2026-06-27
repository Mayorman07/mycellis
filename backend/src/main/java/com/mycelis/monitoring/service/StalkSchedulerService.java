package com.mycelis.monitoring.service;

import com.mycelis.shared.config.MonitoringProperties;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.engine.PulseEngine;
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

@Slf4j
@Service
public class StalkSchedulerService {

    private final StalkClaimService stalkClaimService;
    private final PulseEngine pulseEngine;
    private final MonitoringProperties monitoringProperties;
    private final MeterRegistry meterRegistry;

    private final Counter tickProcessedCounter;
    private final Counter tickUpdatedCounter;
    private final Counter tickEmptyCounter;
    private final Counter tickFailedCounter;
    private final Timer tickDurationTimer;

    public StalkSchedulerService(StalkClaimService stalkClaimService,
                                 PulseEngine pulseEngine,
                                 MonitoringProperties monitoringProperties,
                                 MeterRegistry meterRegistry) {
        this.stalkClaimService = stalkClaimService;
        this.pulseEngine = pulseEngine;
        this.monitoringProperties = monitoringProperties;
        this.meterRegistry = meterRegistry;

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

        this.tickDurationTimer = Timer.builder("app.scheduler.tick.duration")
                .description("Duration of a complete scheduler tick cycle")
                .register(meterRegistry);
    }

    /**
     * Scheduler tick. Runs on the Spring scheduling-1 thread.
     *
     * <p>NO @Transactional here. The claim phase opens its own transaction inside
     * StalkClaimService (via proxy). The HTTP dispatch in Phase 2 runs without any
     * transaction, so row locks are released long before HTTP I/O begins.</p>
     */
    @Scheduled(fixedDelayString = "#{@monitoringProperties.schedulerTickInterval.toMillis()}")
    public void runCheckCycle() {
        Timer.Sample sample = Timer.start(meterRegistry);
        Instant cycleStart = Instant.now();
        log.debug("Scheduler tick started");

        try {
            // Phase 1: CLAIM (own transaction inside StalkClaimService, commits before returning)
            List<Stalk> dueStalks = stalkClaimService.claimDueStalks(
                    cycleStart,
                    monitoringProperties.getMaxBatchSize()
            );

            if (dueStalks.isEmpty()) {
                log.trace("No stalks due in this tick");
                tickEmptyCounter.increment();
                return;
            }

            log.info("Processing batch of {} stalks (max: {})",
                    dueStalks.size(),
                    monitoringProperties.getMaxBatchSize());

            // Phase 2: DISPATCH (no transaction, no locks held)
            pulseEngine.executeCycle(dueStalks);

            tickProcessedCounter.increment(dueStalks.size());
            tickUpdatedCounter.increment(dueStalks.size());

            Duration elapsed = Duration.between(cycleStart, Instant.now());
            log.info("Tick completed: elapsed={}, processed={}, updated={}",
                    elapsed, dueStalks.size(), dueStalks.size());

        } catch (Exception e) {
            log.error("Scheduler tick failed", e);
            tickFailedCounter.increment();
        } finally {
            sample.stop(tickDurationTimer);
        }
    }

    /**
     * Manual trigger for testing or emergency re-checks.
     * Not exposed via API in production.
     */
    @Transactional
    public void triggerManualCheck(List<Stalk> stalks) {
        if (stalks == null || stalks.isEmpty()) {
            log.debug("Manual check triggered with empty stalk list");
            return;
        }

        log.info("Manual check triggered for {} stalks", stalks.size());
        pulseEngine.executeCycle(stalks);
    }
}