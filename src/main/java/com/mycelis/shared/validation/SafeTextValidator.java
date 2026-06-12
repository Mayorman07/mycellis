package com.mycelis.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;


public class SafeTextValidator implements ConstraintValidator<SafeText, String> {

    /**
     * INTERNATIONALIZED SAFE PATTERN
     * \p{L}  -> Matches any Unicode letter (including é, ç, ü, ñ, etc.)
     * \p{N}  -> Matches any Unicode number
     * \s     -> Matches spaces
     * \-.,'  -> Matches safe punctuation (hyphens, periods, commas, apostrophes)
     * #&/    -> Matches common address symbols (e.g., Apt #4, B&B, c/o)
     * * Explicitly rejects <, >, {, }, =, and script tags to prevent XSS.
     */
    private static final String SAFE_PATTERN = "^[\\p{L}\\p{N}\\s\\-.,'#&/]+$";
    private static final Pattern PATTERN = Pattern.compile(SAFE_PATTERN);


    @Override
    public void initialize(SafeText constraintAnnotation) {
    }


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }


        return PATTERN.matcher(value).matches();
    }
}
