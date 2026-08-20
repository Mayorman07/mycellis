package com.mycelis.shared.exception;

/**
 * Thrown when a stalk's (normalized) URL already exists within the same
 * organization. Maps to HTTP 409 in GlobalExceptionHandler.
 */
public class DuplicateStalkUrlException extends RuntimeException {
    public DuplicateStalkUrlException(String message) {
        super(message);
    }
}
