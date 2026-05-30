package com.mycelis.listener;

import com.mycelis.event.PulseCheckedEvent;
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
public class StalkStateListener {

    private final StalkService stalkService;

    @Async("stalkExecutor")  // ← Separate thread pool for state updates
    @EventListener
    @Transactional
    public void handlePulseChecked(PulseCheckedEvent event) {
        log.info("🎯 EVENT RECEIVED! stalkId={}, success={}",
                event.getStalkId(), event.isSuccess());
        try {
            log.info("📊 Calling updateMetricsAndTransitionState for {}", event.getStalkId());
            // Tenant isolation check (defensive)
            stalkService.updateMetricsAndTransitionState(
                    event.getStalkId(),
                    event.getCheckedAt()
            );
            log.debug("Stalk state updated: stalkId={}", event.getStalkId());

        } catch (Exception e) {
            log.error("Failed to update stalk state for {}: {}",
                    event.getStalkId(), e.getMessage(), e);
            // Don't retry - next pulse will catch up
        }
    }
}