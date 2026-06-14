package com.mycelis.notification.event;

/**
 * Domain event published when a new user account is created.
 * <p>
 * Lives in the notification module because notification is the primary
 * consumer. If multiple modules later consume it (analytics, audit, etc.),
 * we'll promote it to com.mycelis.shared.event.
 * <p>
 * Carries only what listeners need — no entity references (keeps modules
 * decoupled and makes future RabbitMQ migration a serialization-only change).
 */
public record UserCreatedEvent(
        String email,
        String firstName,
        String verificationToken
) {}