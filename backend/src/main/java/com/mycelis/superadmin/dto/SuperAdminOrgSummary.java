package com.mycelis.superadmin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Row shape for GET /api/super-admin/organizations — one row per org across
 * every tenant, with member/stalk counts aggregated in SQL rather than
 * fetched and counted in Java (see OrganizationRepository.findAllWithCounts).
 */
public record SuperAdminOrgSummary(
        UUID id,
        String name,
        String slug,
        String planTier,
        long memberCount,
        long stalkCount,
        Instant createdAt
) {}
