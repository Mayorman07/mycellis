package com.mycelis.user.model.response;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        UUID id,
        String email,
        Set<String> roles
) {}