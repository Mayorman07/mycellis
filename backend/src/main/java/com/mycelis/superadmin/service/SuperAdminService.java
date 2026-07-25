package com.mycelis.superadmin.service;

import com.mycelis.monitoring.dto.responses.BatchPulsesResponse;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.superadmin.dto.SuperAdminOrgDetail;
import com.mycelis.superadmin.dto.SuperAdminOrgSummary;
import com.mycelis.superadmin.dto.SuperAdminUserSummary;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only, cross-tenant views for super admins. Every method here
 * deliberately bypasses the tenant scoping that regular services enforce —
 * safe only because every call site is gated by super-admin authorization
 * (see SuperAdminController's {@code @PreAuthorize}).
 */
public interface SuperAdminService {

    List<SuperAdminOrgSummary> getAllOrganizations();

    List<SuperAdminUserSummary> getAllUsers();

    List<StalkResponse> getOrganizationStalks(UUID organizationId);

    SuperAdminOrgDetail getOrganizationDetail(UUID organizationId);

    /** Recent pulses for any stalks, regardless of owning organization — powers the cross-tenant sparkline view. */
    BatchPulsesResponse getBatchPulses(Set<UUID> stalkIds, int limit);
}
