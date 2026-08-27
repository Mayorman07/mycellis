package com.mycelis.shared.ratelimit;

/**
 * Not thrown by {@link Bucket4jRateLimitFilter} today — the filter writes its
 * 429 response directly since it runs outside Spring MVC's exception
 * machinery. This exists so a future service-layer rate limit (one that
 * genuinely throws mid-request, inside a controller method) has a handler
 * ready in GlobalExceptionHandler without needing a refactor later.
 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
