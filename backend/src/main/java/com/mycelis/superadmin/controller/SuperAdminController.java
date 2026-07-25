package com.mycelis.superadmin.controller;

import com.mycelis.monitoring.dto.responses.BatchPulsesResponse;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.superadmin.dto.SuperAdminOrgDetail;
import com.mycelis.superadmin.dto.SuperAdminOrgSummary;
import com.mycelis.superadmin.dto.SuperAdminUserSummary;
import com.mycelis.superadmin.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only, cross-tenant admin views. No endpoint here is public — every
 * one requires {@code principal.superAdmin}, the first super-admin
 * authorization pattern in this codebase (see User.isSuperAdmin javadoc:
 * it supersedes the legacy SUPER_ADMIN role for authorization checks).
 */
@RestController
@RequestMapping("/api/super-admin")
@Validated
@RequiredArgsConstructor
@Tag(name = "Super Admin", description = "Read-only cross-tenant views for super admins")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @Operation(summary = "List all organizations across every tenant")
    @GetMapping("/organizations")
    @PreAuthorize("@superAdminSecurity.check(authentication)")
    public ResponseEntity<List<SuperAdminOrgSummary>> getAllOrganizations() {
        return ResponseEntity.ok(superAdminService.getAllOrganizations());
    }

    @Operation(summary = "List all users across every tenant")
    @GetMapping("/users")
    @PreAuthorize("@superAdminSecurity.check(authentication)")
    public ResponseEntity<List<SuperAdminUserSummary>> getAllUsers() {
        return ResponseEntity.ok(superAdminService.getAllUsers());
    }

    @Operation(summary = "Retrieve one organization's full detail, including its members")
    @GetMapping("/organizations/{orgId}")
    @PreAuthorize("@superAdminSecurity.check(authentication)")
    public ResponseEntity<SuperAdminOrgDetail> getOrganizationDetail(@PathVariable UUID orgId) {
        return ResponseEntity.ok(superAdminService.getOrganizationDetail(orgId));
    }

    @Operation(summary = "Retrieve any organization's stalks, bypassing tenant scoping")
    @GetMapping("/organizations/{orgId}/stalks")
    @PreAuthorize("@superAdminSecurity.check(authentication)")
    public ResponseEntity<List<StalkResponse>> getOrganizationStalks(@PathVariable UUID orgId) {
        return ResponseEntity.ok(superAdminService.getOrganizationStalks(orgId));
    }

    @Operation(summary = "Retrieve recent pulses across any stalks, bypassing tenant scoping")
    @GetMapping("/stalks/pulses/batch")
    @PreAuthorize("@superAdminSecurity.check(authentication)")
    public ResponseEntity<BatchPulsesResponse> getBatchPulses(
            @Parameter(description = "Comma-separated stalk UUIDs (max 100)")
            @RequestParam @Size(max = 100, message = "stalkIds must not exceed 100 ids per request") Set<UUID> stalkIds,
            @Parameter(description = "Pulses per stalk (1-100)", example = "40")
            @RequestParam(defaultValue = "40") @Min(1) @Max(100) int limit) {

        return ResponseEntity.ok(superAdminService.getBatchPulses(stalkIds, limit));
    }
}
