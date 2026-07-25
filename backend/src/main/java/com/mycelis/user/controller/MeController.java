package com.mycelis.user.controller;

import com.mycelis.user.model.request.UpdateAlertPreferencesRequest;
import com.mycelis.user.model.response.MeResponse;
import com.mycelis.user.service.MeService;
import com.mycelis.user.security.MycelisUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "Update the authenticated user's alert preferences")
    @PatchMapping(path = "/alert-preferences", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> updateAlertPreferences(
            @AuthenticationPrincipal MycelisUserPrincipal principal,
            @Valid @RequestBody UpdateAlertPreferencesRequest request) {
        return ResponseEntity.ok(meService.updateAlertPreferences(principal.getId(), request));
    }
}