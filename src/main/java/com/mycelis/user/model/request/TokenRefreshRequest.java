package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenRefreshRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {}