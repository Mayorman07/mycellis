package com.mycelis.listener;

import com.mycelis.entity.Stalk;
import com.mycelis.event.PulseCheckedEvent;
import com.mycelis.entity.Pulse;
import com.mycelis.repository.PulseRepository;
import com.mycelis.repository.StalkRepository;
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

    @Async("pulseExecutor")  // ← Dedicated thread pool
    @EventListener
    @Transactional
    public void handlePulseChecked(PulseCheckedEvent event) {
        try {

            Stalk stalk = stalkRepository.findById(event.getStalkId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Stalk not found: " + event.getStalkId()));

            Pulse pulse = Pulse.builder()
                    .stalk(stalk)
                    .statusCode(event.getStatusCode())
                    .latencyMs(event.getLatencyMs())
                    .isSuccess(event.isSuccess())
                    .errorMessage(event.getErrorMessage())
                    .createdAt(event.getCheckedAt())
                    .build();

            pulseRepository.save(pulse);
            log.debug("Pulse persisted: stalkId={}, status={}",
                    event.getStalkId(), event.getStatusCode());

        } catch (Exception e) {
            // Log but don't retry - occasional loss is acceptable for metrics
            log.warn("Failed to persist pulse for stalk {}: {}",
                    event.getStalkId(), e.getMessage());
        }
    }
}
