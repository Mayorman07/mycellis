package com.mycelis.user.model.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response for GET /api/me — the authenticated user's identity + org context.
 *
 * <p>Consumed by the frontend on app load and after page refresh to hydrate
 * the app shell (who am I, which org, what can I do). Kept minimal — heavier
 * details like billing state or team roster belong on their own endpoints.</p>
 */
public record MeResponse(
        UserInfo user,
        OrganizationInfo organization
) {
    public record UserInfo(
            UUID id,
            String email,
            String firstName,
            String lastName,
            List<String> roles,
            Instant createdAt,
            String alertEmail,
            boolean alertsEnabled
    ) {}

    public record OrganizationInfo(
            UUID id,
            String name,
            String slug,
            String planTier,
            long memberCount
    ) {}
}