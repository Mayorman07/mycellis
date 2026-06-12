package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycelis.shared.validation.SafeText;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateUserRequest(

        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        @SafeText(message = "First name contains invalid or malicious characters")
        String firstName,

        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        @SafeText(message = "Last name contains invalid or malicious characters")
        String lastName,

        @Pattern(
                regexp = "^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Invalid gender selection"
        )
        String gender,

        @Pattern(regexp = "^\\+?[0-9]{11,15}$", message = "Mobile number must be between 11 and 15 digits")
        String mobileNumber

) {}