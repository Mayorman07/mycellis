package com.mycelis.user.model.response;

import com.mycelis.user.constant.Status;

import java.time.Instant;
import java.util.Set;

public record CreateUserResponse(
        String userId,
        String firstName,
        String lastName,
        String email,
        Status status,
        Set<String> roles,
        Instant createdAt
) {}