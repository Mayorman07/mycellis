package com.mycelis.monitoring.controller;



import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.monitoring.service.StalkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST gateway for Stalk lifecycle management.
 * Enforces input validation, tenant isolation, and consistent HTTP semantics.
 *
 * <p>Production note: {@code userId} is currently accepted as a request parameter
 * for development testing. In production, this will be resolved automatically via
 * Spring Security {@code @AuthenticationPrincipal} or a gateway-injected {@code @RequestAttribute}.</p>
 *
 */
@RestController
@RequestMapping("/api/v1/stalks")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stalk Management", description = "Endpoints for creating, retrieving, and managing monitoring targets")
public class StalkController {

    private final StalkService stalkService;

    @Operation(summary = "Register a new monitoring target", description = "Creates a stalk with validated configuration. Returns 201 Created.")
    @PostMapping
    public ResponseEntity<StalkResponse> createStalk(
            @Parameter(description = "Tenant identifier (dev-only; will be auto-resolved via auth in prod)", required = true)
            @RequestParam UUID userId,
            @Valid @RequestBody CreateStalkRequest request) {

        log.info("Creating monitoring target for userId={}, url={}", userId, request.getUrl());
        StalkResponse response = stalkService.createStalk(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieve stalk by identifier", description = "Returns full stalk representation including cached metrics and state.")
    @GetMapping("/{id}")
    public ResponseEntity<StalkResponse> getStalk(
            @RequestParam UUID userId,
            @PathVariable UUID id) {

        return ResponseEntity.ok(stalkService.getStalkById(userId, id));
    }

    @Operation(summary = "List stalks with pagination", description = "Returns paginated stalks sorted by creation date (descending).")
    @GetMapping
    public ResponseEntity<Page<StalkResponse>> listStalks(
            @RequestParam UUID userId,
            @PageableDefault(size = 20, sort = "createdAt", direction =Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(stalkService.getAllStalks(userId, pageable));
    }

    @Operation(summary = "Update stalk configuration", description = "Modifies interval, timeout, or nickname. Preserves existing metrics and state.")
    @PutMapping("/{id}")
    public ResponseEntity<StalkResponse> updateStalk(
            @RequestParam UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateStalkRequest request) {

        log.info("Updating configuration for stalkId={}", id);
        return ResponseEntity.ok(stalkService.updateConfiguration(userId, id, request));
    }

    @Operation(summary = "Delete monitoring target", description = "Permanently removes stalk and cascades to associated pulse records.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStalk(
            @RequestParam UUID userId,
            @PathVariable UUID id) {

        log.info("Deleting monitoring target: stalkId={}", id);
        stalkService.deleteStalk(userId, id);
        return ResponseEntity.noContent().build();
    }
}