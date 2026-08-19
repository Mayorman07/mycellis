package com.mycelis.monitoring.security;

/**
 * Thrown when a stalk URL resolves to a blocked internal/private network
 * target. Maps to HTTP 400 in GlobalExceptionHandler.
 */
public class UnsafeUrlException extends RuntimeException {
    public UnsafeUrlException(String message) {
        super(message);
    }
}
