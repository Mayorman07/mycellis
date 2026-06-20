package com.mycelis.notification.event;

/**
 * Fired AFTER a password reset request transaction commits.
 * Listener consumes this to send the reset email.
 */
public record PasswordResetRequestedEvent(
        String email,
        String firstName,
        String resetToken
) {}