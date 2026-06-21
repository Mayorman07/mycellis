package com.mycelis.monitoring.controller;

import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.monitoring.service.StalkService;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST gateway for Stalk lifecycle management.
 * Tenant identity is resolved from the authenticated principal; never from request parameters.
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

        UUID userId = principal.getId();
        log.info("Creating monitoring target for userId={}, url={}", userId, request.getUrl());
        StalkResponse response = stalkService.createStalk(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Retrieve stalk by identifier")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StalkResponse> getStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id) {

        return ResponseEntity.ok(stalkService.getStalkById(principal.getId(), id));
    }

    @Operation(summary = "List stalks with pagination")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<StalkResponse>> listStalks(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(stalkService.getAllStalks(principal.getId(), pageable));
    }

    @Operation(summary = "Update stalk configuration")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StalkResponse> updateStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateStalkRequest request) {

        log.info("Updating configuration for stalkId={}", id);
        return ResponseEntity.ok(stalkService.updateConfiguration(principal.getId(), id, request));
    }

    @Operation(summary = "Delete monitoring target")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteStalk(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @PathVariable UUID id) {

        log.info("Deleting monitoring target: stalkId={}", id);
        stalkService.deleteStalk(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}