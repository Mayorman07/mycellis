package com.mycelis.user.model.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String userId,
        long expiresIn
) {}