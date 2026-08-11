package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for updating the two alert-preference fields on the authenticated
 * user. alertEmail null/blank means "clear the override, fall back to the
 * primary email" — same optional-override semantics as User.alertEmail.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateAlertPreferencesRequest(
        @Email(message = "Must be a well-formed email address")
        // Unlike the other @Email fields, this one has no @NotBlank — blank/null
        // is a valid, meaningful input ("clear the override", see above), so the
        // pattern allows an empty string in addition to a well-formed address
        // rather than rejecting it outright like the other DTOs do.
        @Pattern(
                regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Must be a well-formed email address"
        )
        String alertEmail,

        @NotNull(message = "alertsEnabled is required")
        Boolean alertsEnabled
) {}
