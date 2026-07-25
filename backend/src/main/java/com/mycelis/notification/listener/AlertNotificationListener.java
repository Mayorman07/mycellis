package com.mycelis.notification.listener;

import com.mycelis.notification.event.StalkDownEvent;
import com.mycelis.notification.event.StalkRecoveryEvent;
import com.mycelis.notification.service.EmailService;
import com.mycelis.notification.template.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Translates alert domain events into emails.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) ensures the email only
 * fires after AlertEngine's tick transaction commits — same guarantee as
 * UserNotificationListener/PasswordResetNotificationListener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotificationListener {

    private final EmailService emailService;
    private final EmailTemplateRenderer templateRenderer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStalkDown(StalkDownEvent event) {
        String html = templateRenderer.render("email/alert-down", Map.of(
                "nickname", event.nickname(),
                "url", event.url()
        ));

        emailService.send(
                event.recipientEmail(),
                "Alert: " + event.nickname() + " is down",
                html
        );

        log.info("Down alert email queued for {}", event.recipientEmail());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStalkRecovery(StalkRecoveryEvent event) {
        String duration = formatDuration(event.downtimeSeconds());

        String html = templateRenderer.render("email/alert-recovery", Map.of(
                "nickname", event.nickname(),
                "url", event.url(),
                "duration", duration
        ));

        emailService.send(
                event.recipientEmail(),
                "Recovered: " + event.nickname() + " is back online after " + duration,
                html
        );

        log.info("Recovery alert email queued for {}", event.recipientEmail());
    }

    /** e.g. 45 -> "45 seconds", 840 -> "14 minutes", 7383 -> "2 hours 3 minutes". */
    private String formatDuration(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + " second" + (totalSeconds == 1 ? "" : "s");
        }

        long totalMinutes = totalSeconds / 60;
        if (totalMinutes < 60) {
            return totalMinutes + " minute" + (totalMinutes == 1 ? "" : "s");
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        String hoursPart = hours + " hour" + (hours == 1 ? "" : "s");
        if (minutes == 0) {
            return hoursPart;
        }
        return hoursPart + " " + minutes + " minute" + (minutes == 1 ? "" : "s");
    }
}
