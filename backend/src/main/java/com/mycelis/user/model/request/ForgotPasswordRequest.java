package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForgotPasswordRequest(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Must be a well-formed email address")
        String email

) {}