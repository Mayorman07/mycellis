package com.mycelis.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class VerificationController {

    private final UserService userService;
    @GetMapping("/verify")
    public ResponseEntity<Object> verifyUser(@RequestParam("token") String token) {
        boolean isVerified = userService.verifyUser(token);

        if (isVerified) {
            return ResponseEntity.ok(java.util.Map.of("message", "Verified"));
        } else {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Expired"));
        }
    }
}
