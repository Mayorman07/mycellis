package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

/**
 * Payload for updating the two alert-preference fields on the authenticated
 * user. alertEmail null/blank means "clear the override, fall back to the
 * primary email" — same optional-override semantics as User.alertEmail.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateAlertPreferencesRequest(
        @Email(message = "Must be a well-formed email address")
        String alertEmail,

        @NotNull(message = "alertsEnabled is required")
        Boolean alertsEnabled
) {}
