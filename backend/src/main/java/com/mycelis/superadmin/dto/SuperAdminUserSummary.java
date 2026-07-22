package com.mycelis.superadmin.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Row shape for GET /api/super-admin/users — one row per user across every
 * tenant. organizationName comes from the user's PRIMARY membership — a user
 * with multiple memberships still shows exactly one org here.
 */
public record SuperAdminUserSummary(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String organizationName,
        Set<String> roles,
        String status,
        Instant createdAt
) {}
