package com.mycelis.shared.exception;

/**
 * Thrown when an organization has reached its stalk quota. Maps to HTTP 409
 * in GlobalExceptionHandler.
 */
public class StalkQuotaExceededException extends RuntimeException {
    public StalkQuotaExceededException(String message) {
        super(message);
    }
}
