package com.mycelis.controller;

import com.mycelis.model.dto.responses.PulseResponse;
import com.mycelis.model.dto.responses.UptimeResponse;
import com.mycelis.service.PulseService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * REST gateway for Pulse diagnostics and time-series analytics.
 * Nested under stalks to enforce resource hierarchy and ownership validation.
 *
 */
@RestController
@RequestMapping("/api/v1/stalks/{stalkId}/pulses")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pulse Analytics", description = "Endpoints for retrieving health check history and uptime metrics")
public class PulseController {

    private final PulseService pulseService;

    @Operation(summary = "Retrieve recent pulses", description = "Returns the most recent health checks for real-time dashboard rendering.")
    @GetMapping
    public ResponseEntity<List<PulseResponse>> getRecentPulses(
            @PathVariable UUID stalkId,
            @Parameter(description = "Number of records to return (1-200)", example = "50")
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {

        log.debug("Fetching recent pulses for stalkId={}, limit={}", stalkId, limit);
        return ResponseEntity.ok(pulseService.getRecentPulses(stalkId, limit));
    }

    @Operation(summary = "Retrieve paginated pulse history", description = "Returns historical checks with cursor-based pagination for trend analysis.")
    @GetMapping("/history")
    public ResponseEntity<Page<PulseResponse>> getPulseHistory(
            @PathVariable UUID stalkId,
            @PageableDefault(size = 50, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(pulseService.getPulseHistory(stalkId, pageable));
    }

    @Operation(summary = "Calculate uptime percentage", description = "Computes success ratio over an ISO-8601 duration window (e.g., P7D, P30D).")
    @GetMapping("/uptime")
    public ResponseEntity<UptimeResponse> getUptime(
            @PathVariable UUID stalkId,
            @Parameter(description = "ISO-8601 duration window (e.g., P1D, P7D, P30D)", example = "P7D")
            @RequestParam(defaultValue = "P7D")
            @Pattern(regexp = "^P(\\d+D|\\d+W|\\d+M)$", message = "Window must be a valid ISO-8601 duration")
            String window) {

        return ResponseEntity.ok(pulseService.getUptimeByWindow(stalkId, window));
    }

}
