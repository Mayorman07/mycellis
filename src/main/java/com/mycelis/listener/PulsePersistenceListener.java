package com.mycelis.listener;

import com.mycelis.entity.Pulse;
import com.mycelis.entity.Stalk;
import com.mycelis.event.PulseCheckedEvent;
import com.mycelis.repository.PulseRepository;
import com.mycelis.repository.StalkRepository;
import com.mycelis.service.StalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
            // 1. Fetch Stalk Entity
            Stalk stalk = stalkRepository.findById(event.getStalkId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stalk not found: " + event.getStalkId()));

            // 2. Create and Save Pulse
            Pulse pulse = Pulse.builder()
                    .stalk(stalk)
                    .statusCode(event.getStatusCode())
                    .latencyMs(event.getLatencyMs())
                    .isSuccess(event.isSuccess())
                    .errorMessage(event.getErrorMessage())
                    .build();

            pulseRepository.save(pulse);
            log.debug("Pulse persisted: stalkId={}, status={}",
                    event.getStalkId(), event.getStatusCode());

            // 3. Update Stalk State immediately after saving pulse
            // Since we are in the same transaction, the count query in the service
            stalkService.updateMetricsAndTransitionState(
                    event.getStalkId(),
                    event.getCheckedAt()
            );

        } catch (Exception e) {
            log.warn("Failed to persist pulse or update state for stalk {}: {}",
                    event.getStalkId(), e.getMessage());
        }
    }
}