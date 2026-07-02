package com.mycelis.monitoring.controller;

import com.mycelis.monitoring.dto.responses.PulseResponse;
import com.mycelis.monitoring.dto.responses.UptimeResponse;
import com.mycelis.monitoring.service.PulseService;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST gateway for Pulse diagnostics and time-series analytics.
 * Service layer verifies stalk tenancy against the authenticated principal's organization.
 */
@RestController
@RequestMapping("/api/stalks/{stalkId}/pulses")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pulse Analytics", description = "Endpoints for retrieving health check history and uptime metrics")
public class PulseController {

    private final PulseService pulseService;

    @Operation(summary = "Retrieve recent pulses")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PulseResponse>> getRecentPulses(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID stalkId,
            @Parameter(description = "Number of records to return (1-200)", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {

        log.debug("Fetching recent pulses for stalkId={}, orgId={}, limit={}",
                stalkId, principal.getOrganizationId(), limit);
        return ResponseEntity.ok(pulseService.getRecentPulses(principal.getOrganizationId(), stalkId, limit));
    }

    @Operation(summary = "Retrieve paginated pulse history")
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PulseResponse>> getPulseHistory(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID stalkId,
            @PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(pulseService.getPulseHistory(principal.getOrganizationId(), stalkId, pageable));
    }

    @Operation(summary = "Calculate uptime percentage")
    @GetMapping("/uptime")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UptimeResponse> getUptime(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID stalkId,
            @Parameter(description = "ISO-8601 duration window (e.g., P1D, P7D, P30D)", example = "P7D")
            @RequestParam(defaultValue = "P7D")
            @Pattern(regexp = "^P(\\d+D|\\d+W|\\d+M)$", message = "Window must be a valid ISO-8601 duration")
            String window) {

        return ResponseEntity.ok(pulseService.getUptimeByWindow(principal.getOrganizationId(), stalkId, window));
    }
}