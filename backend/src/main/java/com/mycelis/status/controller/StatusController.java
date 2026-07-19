package com.mycelis.status.controller;

import com.mycelis.status.dto.PublicStatusResponse;
import com.mycelis.status.service.StatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated gateway for workspace status pages.
 *
 * <p>No {@code @PreAuthorize} here by design — this is meant to be visited
 * by anyone with the link, logged in or not. See SecurityConfig for the
 * matching {@code /api/status/**} permitAll rule.</p>
 */
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
@Tag(name = "Status", description = "Public status pages")
public class StatusController {

    private final StatusService statusService;

    @Operation(summary = "Retrieve a workspace's public status page")
    @GetMapping("/{slug}")
    public ResponseEntity<PublicStatusResponse> getPublicStatus(@PathVariable String slug) {
        return ResponseEntity.ok(statusService.getPublicStatus(slug));
    }
}
