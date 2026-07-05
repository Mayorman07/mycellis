package com.mycelis.monitoring.controller;

import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.dto.responses.BatchPulsesResponse;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.monitoring.service.StalkService;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * REST gateway for Stalk lifecycle management.
 *
 * <p>Tenant identity is derived from the authenticated principal's {@code organizationId},
 * never from request parameters. This is the ONLY layer that reads the principal;
 * downstream services accept a plain {@code UUID organizationId} argument.</p>
 */
@RestController
@RequestMapping("/api/stalks")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stalk Management", description = "Endpoints for creating, retrieving, and managing monitoring targets")
public class StalkController {

    private final StalkService stalkService;

    @Operation(summary = "Register a new monitoring target")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StalkResponse> createStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @Valid @RequestBody CreateStalkRequest request) {

        log.info("Creating stalk: orgId={}, createdBy={}, url={}",
                principal.getOrganizationId(), principal.getId(), request.getUrl());

        StalkResponse response = stalkService.createStalk(
                principal.getOrganizationId(), principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieve stalk by identifier")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StalkResponse> getStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id) {

        return ResponseEntity.ok(stalkService.getStalkById(principal.getOrganizationId(), id));
    }

    @Operation(summary = "List stalks for the authenticated user's organization")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<StalkResponse>> listStalks(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(stalkService.getAllStalks(principal.getOrganizationId(), pageable));
    }

    @Operation(summary = "Retrieve recent pulses across multiple stalks in one request")
    @GetMapping("/pulses/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchPulsesResponse> getBatchPulses(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @Parameter(description = "Comma-separated stalk UUIDs (max 100)")
            @RequestParam @Size(max = 100, message = "stalkIds must not exceed 100 ids per request") Set<UUID> stalkIds,
            @Parameter(description = "Pulses per stalk (1-100)", example = "40")
            @RequestParam(defaultValue = "40") @Min(1) @Max(100) int limit) {

        log.info("Batch pulses requested: orgId={}, stalkCount={}, limit={}",
                principal.getOrganizationId(), stalkIds.size(), limit);

        return ResponseEntity.ok(stalkService.getBatchPulses(stalkIds, limit, principal.getOrganizationId()));
    }

    @Operation(summary = "Update stalk configuration")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StalkResponse> updateStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateStalkRequest request) {

        log.info("Updating stalk configuration: stalkId={}, orgId={}", id, principal.getOrganizationId());
        return ResponseEntity.ok(stalkService.updateConfiguration(principal.getOrganizationId(), id, request));
    }

    @Operation(summary = "Delete monitoring target")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id) {

        log.info("Deleting stalk: stalkId={}, orgId={}", id, principal.getOrganizationId());
        stalkService.deleteStalk(principal.getOrganizationId(), id);
        return ResponseEntity.noContent().build();
    }
}