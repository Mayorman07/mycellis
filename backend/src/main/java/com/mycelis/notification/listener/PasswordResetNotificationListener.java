package com.mycelis.notification.listener;

import com.mycelis.notification.event.PasswordResetRequestedEvent;
import com.mycelis.notification.service.EmailService;
import com.mycelis.notification.template.EmailTemplateRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Sends the password reset email after the request transaction commits.
 * If the tx rolls back, no email is sent — same safety guarantee as
 * UserNotificationListener.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetNotificationListener {

    private final EmailService emailService;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${mycelis.app.frontend-url}")
    private String frontendUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        String resetLink = frontendUrl + "/reset-password?token=" + event.resetToken();

        String html = templateRenderer.render("email/password-reset-email", Map.of(
                "firstName", event.firstName(),
                "resetLink", resetLink
        ));

        emailService.send(
                event.email(),
                "Reset your Mycellis password",
                html
        );

        log.info("Password reset email queued for {}", event.email());
    }
}