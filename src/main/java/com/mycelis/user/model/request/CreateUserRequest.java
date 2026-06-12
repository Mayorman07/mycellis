package com.mycelis.user.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mycelis.shared.validation.SafeText;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateUserRequest(

        @NotBlank(message = "First name cannot be empty")
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        @SafeText(message = "First name contains invalid or malicious characters")
        String firstName,

        @NotBlank(message = "Last name cannot be empty")
        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        @SafeText(message = "Last name contains invalid or malicious characters")
        String lastName,

        @NotBlank(message = "Email cannot be empty")
        @Email(message = "Must be a well-formed email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password cannot be empty")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password,

        @NotBlank(message = "Gender cannot be empty")
        @Pattern(
                regexp = "^(MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY)$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Invalid gender selection"
        )
        String gender,

        @NotBlank(message = "Mobile number cannot be empty")
        @Pattern(regexp = "^\\+?[0-9]{11,15}$", message = "Mobile number must be between 11 and 15 digits")
        String mobileNumber

) {}