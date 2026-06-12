package com.mycelis.user.model.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {}