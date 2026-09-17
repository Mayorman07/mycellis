package com.mycelis.alert.service;

import com.mycelis.alert.repository.AlertRepository;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure unit test — no Spring context, all collaborators mocked.
 */
@ExtendWith(MockitoExtension.class)
class AlertEngineTest {

    @Mock
    private StalkRepository stalkRepository;
    @Mock
    private PulseRepository pulseRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AlertEngine alertEngine;

    private void setUp() {
        alertEngine = new AlertEngine(stalkRepository, pulseRepository, alertRepository, userRepository, eventPublisher);
    }

    @Test
    void awakeningStalkIsSkippedEvenWithLongRunningFailureHistory() {
        setUp();

        Stalk stalk = Stalk.builder()
                .id(UUID.randomUUID())
                .reliabilityState(ReliabilityState.AWAKENING)
                .build();

        Instant now = Instant.now();
        List<Pulse> failingPulses = List.of(
                Pulse.builder().isSuccess(false).createdAt(now).build(),
                Pulse.builder().isSuccess(false).createdAt(now.minus(Duration.ofMinutes(3))).build(),
                Pulse.builder().isSuccess(false).createdAt(now.minus(Duration.ofMinutes(6))).build()
        );

        when(stalkRepository.findByIsActiveTrue()).thenReturn(List.of(stalk));
        // Armed to prove the AWAKENING guard — not the pulse-history logic — is what
        // prevents fireDown. lenient() because the guard should return before
        // evaluateStalk ever consults this stub.
        lenient().when(pulseRepository.findTopByStalkIdOrderByCreatedAtDesc(eq(stalk.getId()), any(Pageable.class)))
                .thenReturn(failingPulses);

        alertEngine.checkForAlertableTransitions();

        verify(alertRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
