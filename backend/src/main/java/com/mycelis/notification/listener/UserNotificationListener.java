package com.mycelis.notification.listener;

import com.mycelis.notification.event.UserCreatedEvent;
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
 * Translates domain events into emails.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) ensures the email
 * only fires after the user-creation transaction commits. If the tx
 * rolls back, no email is sent — preventing "email sent for a user
 * that doesn't exist" bugs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationListener {

    private final EmailService emailService;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${mycelis.app.frontend-url}")
    private String frontendUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        String verifyLink = frontendUrl + "/verify?token=" + event.verificationToken();

        String html = templateRenderer.render("email/verification-email", Map.of(
                "firstName", event.firstName(),
                "verifyLink", verifyLink
        ));

        emailService.send(
                event.email(),
                "Verify your Mycellis email",
                html
        );

        log.info("Verification email queued for {}", event.email());
    }
}