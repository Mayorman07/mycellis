package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeEmailRequest(

        @NotBlank(message = "New email cannot be empty")
        @Email(message = "Must be a well-formed email address")
        @Pattern(
                regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Must be a well-formed email address"
        )
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword

) {}