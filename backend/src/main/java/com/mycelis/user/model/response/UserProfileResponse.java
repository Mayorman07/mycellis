package com.mycelis.user.model.response;

import com.mycelis.user.constant.Status;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Status status,
        String mobileNumber,
        Instant lastLoggedIn,
        Instant createdAt,
        Instant updatedAt
) {}