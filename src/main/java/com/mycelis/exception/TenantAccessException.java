package com.mycelis.exception;

/**
 * Thrown when a tenant attempts to access a resource owned by another tenant.
 * Maps to HTTP 403 Forbidden in GlobalExceptionHandler.
 */
public class TenantAccessException extends RuntimeException {
    public TenantAccessException(String message) {
        super(message);
    }
}
