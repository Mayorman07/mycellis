package com.mycelis.notification.event;

/**
 * Fired AFTER an AlertEngine tick's transaction commits a DOWN alert.
 * Listener consumes this to send the down-alert email.
 */
public record StalkDownEvent(
        String recipientEmail,
        String nickname,
        String url
) {}
