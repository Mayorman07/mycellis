package com.mycelis.alert.service;

import com.mycelis.alert.constant.AlertType;
import com.mycelis.alert.entity.Alert;
import com.mycelis.alert.repository.AlertRepository;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.notification.event.StalkDownEvent;
import com.mycelis.notification.event.StalkRecoveryEvent;
import com.mycelis.user.entity.User;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Minimum viable alerting: fires a DOWN email once a stalk has been
 * continuously failing for {@link #DOWN_THRESHOLD}, and a RECOVERY email the
 * moment it succeeds again. Runs every 60s — a fixed cadence is plenty at
 * beta scale, no need for anything more responsive.
 *
 * <p>"Continuously down" is derived directly from recent Pulse rows (isSuccess
 * + createdAt), not from Stalk.reliabilityState or Stalk.consecutiveFailures.
 * Neither actually models this: reliabilityState's stalk-aggregate value is
 * only ever HEALTHY/DEGRADED/DORMANT (DOWN exists only at per-pulse
 * granularity — see ReliabilityState's own javadoc), and consecutiveFailures
 * is never written anywhere in the codebase today (always 0). Rather than
 * touch either of those — out of scope for this commit — this walks the
 * pulse history directly.</p>
 *
 * <p>Suppression + recovery state live entirely in the alerts table: "most
 * recent alert row for a stalk is a DOWN" means there's an open incident, so
 * no further DOWN fires until a RECOVERY is recorded. No separate in-memory
 * or Stalk-column tracking needed.</p>
 *
 * <p>Disabled entirely under IntegrationTestBase (see its
 * mycelis.alerts.scheduling.enabled=false override) — otherwise every
 * integration test class boots a real, ticking AlertEngine against the
 * shared dev DB and fires real alerts against whatever real stalks happen
 * to have failing pulse history, independent of what any given test is
 * actually checking.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mycelis.alerts.scheduling.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AlertEngine {

    private static final Duration DOWN_THRESHOLD = Duration.ofMinutes(5);
    private static final int PULSE_LOOKBACK_LIMIT = 100;

    private final StalkRepository stalkRepository;
    private final PulseRepository pulseRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void checkForAlertableTransitions() {
        for (Stalk stalk : stalkRepository.findByIsActiveTrue()) {
            try {
                evaluateStalk(stalk);
            } catch (Exception e) {
                // One stalk's failure must not stop the rest of this tick.
                log.error("Alert evaluation failed for stalk {}", stalk.getId(), e);
            }
        }
    }

    private void evaluateStalk(Stalk stalk) {
        if (stalk.getReliabilityState() == ReliabilityState.AWAKENING) {
            return;
        }

        List<Pulse> recentPulses = pulseRepository.findTopByStalkIdOrderByCreatedAtDesc(
                stalk.getId(), PageRequest.of(0, PULSE_LOOKBACK_LIMIT));

        if (recentPulses.isEmpty()) {
            return;
        }

        Alert latestAlert = alertRepository.findTopByStalkIdOrderByFiredAtDesc(stalk.getId()).orElse(null);
        boolean openDownIncident = latestAlert != null && latestAlert.getAlertType() == AlertType.DOWN;
        boolean mostRecentPulseSucceeded = Boolean.TRUE.equals(recentPulses.get(0).getIsSuccess());

        if (openDownIncident) {
            if (mostRecentPulseSucceeded) {
                fireRecovery(stalk, latestAlert);
            }
            // Still down, or just recovered — either way, suppressed: no new DOWN this tick.
            return;
        }

        if (!mostRecentPulseSucceeded && continuouslyDownFor(recentPulses, DOWN_THRESHOLD)) {
            fireDown(stalk);
        }
    }

    /**
     * Walks pulses newest-first, collecting the unbroken run of failures at
     * the front. True only if that run is non-empty and spans at least
     * {@code threshold} of wall-clock time — a handful of failed checks
     * within the first minute of an outage must not fire early, and any
     * success in the run resets it (flapping never accumulates).
     */
    private boolean continuouslyDownFor(List<Pulse> pulsesNewestFirst, Duration threshold) {
        Instant oldestInStreak = null;
        for (Pulse pulse : pulsesNewestFirst) {
            if (Boolean.TRUE.equals(pulse.getIsSuccess())) {
                break;
            }
            oldestInStreak = pulse.getCreatedAt();
        }
        return oldestInStreak != null && !Duration.between(oldestInStreak, Instant.now()).minus(threshold).isNegative();
    }

    private void fireDown(Stalk stalk) {
        Instant now = Instant.now();
        User recipient = resolveRecipient(stalk);
        boolean shouldSend = recipient != null && recipient.isAlertsEnabled();

        Alert alert = Alert.builder()
                .stalkId(stalk.getId())
                .alertType(AlertType.DOWN)
                .firedAt(now)
                .deliveredAt(shouldSend ? now : null)
                .build();
        alertRepository.save(alert);

        if (shouldSend) {
            eventPublisher.publishEvent(new StalkDownEvent(recipientEmail(recipient), stalk.getNickname(), stalk.getUrl()));
        }

        log.info("DOWN alert fired: stalkId={}, delivered={}", stalk.getId(), shouldSend);
    }

    private void fireRecovery(Stalk stalk, Alert downAlert) {
        Instant now = Instant.now();
        int downtimeSeconds = (int) Duration.between(downAlert.getFiredAt(), now).getSeconds();

        User recipient = resolveRecipient(stalk);
        boolean shouldSend = recipient != null && recipient.isAlertsEnabled();

        Alert alert = Alert.builder()
                .stalkId(stalk.getId())
                .alertType(AlertType.RECOVERY)
                .firedAt(now)
                .downtimeSeconds(downtimeSeconds)
                .deliveredAt(shouldSend ? now : null)
                .build();
        alertRepository.save(alert);

        if (shouldSend) {
            eventPublisher.publishEvent(new StalkRecoveryEvent(
                    recipientEmail(recipient), stalk.getNickname(), stalk.getUrl(), downtimeSeconds));
        }

        log.info("RECOVERY alert fired: stalkId={}, downtimeSeconds={}, delivered={}",
                stalk.getId(), downtimeSeconds, shouldSend);
    }

    private User resolveRecipient(Stalk stalk) {
        User user = userRepository.findById(stalk.getCreatedByUserId()).orElse(null);
        if (user == null) {
            log.warn("Stalk {} createdByUserId {} has no matching user — cannot alert",
                    stalk.getId(), stalk.getCreatedByUserId());
        }
        return user;
    }

    private String recipientEmail(User user) {
        String alertEmail = user.getAlertEmail();
        return (alertEmail != null && !alertEmail.isBlank()) ? alertEmail : user.getEmail();
    }
}
