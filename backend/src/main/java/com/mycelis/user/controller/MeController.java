package com.mycelis.user.controller;

import com.mycelis.user.model.response.MeResponse;
import com.mycelis.user.service.MeService;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the authenticated user's identity + organization context.
 * Called by the frontend on app boot and after page refresh.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Session", description = "Authenticated user identity and context")
public class MeController {

    private final MeService meService;

    @Operation(summary = "Get the authenticated user and their organization")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal MycelisUserPrincipal principal) {
        return ResponseEntity.ok(meService.getMe(principal.getId()));
    }
}