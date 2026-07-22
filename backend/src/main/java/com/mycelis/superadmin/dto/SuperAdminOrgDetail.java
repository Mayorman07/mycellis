package com.mycelis.superadmin.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full detail for a single organization, viewed by a super admin —
 * GET /api/super-admin/organizations/{orgId}.
 */
public record SuperAdminOrgDetail(
        UUID id,
        String name,
        String slug,
        String planTier,
        long memberCount,
        long stalkCount,
        Instant createdAt,
        List<MemberSummary> members
) {
    public record MemberSummary(
            UUID userId,
            String name,
            String email,
            String role,
            boolean isPrimary
    ) {}
}
