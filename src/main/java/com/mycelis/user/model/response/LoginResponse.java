package com.mycelis.user.model.response;

import java.util.Set;

public record LoginResponse(
        String userId,
        String email,
        Set<String> roles
) {}