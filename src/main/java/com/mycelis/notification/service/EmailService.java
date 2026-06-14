package com.mycelis.notification.service;

/**
 * Abstraction over email delivery providers.
 * <p>
 * Implementations are selected by Spring profile:
 *   - SmtpEmailService  (dev, local)  → MailHog
 *   - SesEmailService   (prod)        → AWS SES
 */
public interface EmailService {

    /**
     * Send a pre-rendered HTML email. Must not block the caller; impls
     * should run on a separate thread (@Async or the provider's own pool).
     */
    void send(String to, String subject, String htmlBody);
}