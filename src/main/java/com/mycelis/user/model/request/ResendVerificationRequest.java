package com.mycelis.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email address")
        String email
) {}