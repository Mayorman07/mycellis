package com.mycelis.user.model.response;

import com.mycelis.user.constant.Status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CreateUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Status status,
        Set<String> roles,
        Instant createdAt
) {}