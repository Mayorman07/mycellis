package com.mycelis.monitoring.listener;

import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.event.PulseCheckedEvent;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.monitoring.service.StalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single listener for PulseCheckedEvent. Persists the pulse, then updates
 * stalk metrics and state from the new sliding window.
 *
 * <p>Runs async on the {@code pulseExecutor} pool. Each pulse is its own
 * transaction — failure to persist one pulse does not affect others.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PulsePersistenceListener {

    private final PulseRepository pulseRepository;
    private final StalkRepository stalkRepository;
    private final StalkService stalkService;

    @Async("pulseExecutor")
    @EventListener
    @Transactional
    public void handlePulseChecked(PulseCheckedEvent event) {
        try {
            // Lazy proxy: no SELECT issued. JPA only reads stalk.id to set the FK.
            Stalk stalkRef = stalkRepository.getReferenceById(event.getStalkId());

            Pulse pulse = Pulse.builder()
                    .stalk(stalkRef)
                    .statusCode(event.getStatusCode())
                    .latencyMs(event.getLatencyMs())
                    .isSuccess(event.isSuccess())
                    .errorMessage(event.getErrorMessage())
                    .build();

            pulseRepository.save(pulse);
            log.debug("Pulse persisted: stalkId={}, status={}",
                    event.getStalkId(), event.getStatusCode());

            stalkService.updateMetricsAndTransitionState(
                    event.getStalkId(),
                    event.getCheckedAt()
            );

        } catch (Exception e) {
            log.error("Failed to persist pulse or update state for stalk {}: {}",
                    event.getStalkId(), e.getMessage(), e);
        }
    }
}