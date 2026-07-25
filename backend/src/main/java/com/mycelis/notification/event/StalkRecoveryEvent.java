package com.mycelis.notification.event;

/**
 * Fired AFTER an AlertEngine tick's transaction commits a RECOVERY alert.
 * Listener consumes this to send the recovery-alert email.
 */
public record StalkRecoveryEvent(
        String recipientEmail,
        String nickname,
        String url,
        int downtimeSeconds
) {}
