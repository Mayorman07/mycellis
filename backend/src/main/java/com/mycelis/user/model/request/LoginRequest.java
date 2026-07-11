package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LoginRequest(

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Must be a well-formed email address")
        String email,

        @NotBlank(message = "Password cannot be empty")
        String password,

        // Nullable (not primitive) so "absent" is distinguishable from "false" —
        // both mean the same thing today, but keeps the wire contract honest.
        Boolean rememberMe

){}