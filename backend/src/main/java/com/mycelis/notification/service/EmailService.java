package com.mycelis.notification.service;

/**
 * Abstraction over email delivery providers.
 * <p>
 * Implementations are selected by the mycelis.email.provider property
 * (@ConditionalOnProperty), not Spring profile — this lets the provider be
 * overridden independently of dev/prod (e.g. testing Resend locally via an
 * env var without switching profiles):
 *   - SmtpEmailService    (provider=smtp, default) → MailHog in dev
 *   - ResendEmailService  (provider=resend)        → Resend REST API, for prod
 */
public interface EmailService {

    /**
     * Send a pre-rendered HTML email. Must not block the caller; impls
     * should run on a separate thread (@Async or the provider's own pool).
     */
    void send(String to, String subject, String htmlBody);
}