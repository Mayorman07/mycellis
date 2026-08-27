package com.mycelis.shared.exception;

import com.mycelis.monitoring.security.UnsafeUrlException;
import com.mycelis.shared.ratelimit.TooManyRequestsException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_URI = "https://mycellis.dev/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request payload");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation Failed");
        pd.setType(URI.create(BASE_URI + "validation-failed"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Client error: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Request Parameter");
        pd.setType(URI.create(BASE_URI + "invalid-parameter"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    // @Validated on @RequestParam/@PathVariable (e.g. @Min/@Max/@Size) throws this at the
    // method level, not MethodArgumentNotValidException — no existing handler covered it,
    // so violations were previously falling through to the generic 500 handler below.
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request parameter");
        log.warn("Constraint violation: {}", detail);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation Failed");
        pd.setType(URI.create(BASE_URI + "validation-failed"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Required parameter '" + ex.getParameterName() + "' is missing");
        pd.setTitle("Missing Request Parameter");
        pd.setType(URI.create(BASE_URI + "missing-parameter"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: parameter={}, value={}", ex.getName(), ex.getValue());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid parameter format: " + ex.getName());
        pd.setTitle("Malformed Request");
        pd.setType(URI.create(BASE_URI + "malformed-request"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setType(URI.create(BASE_URI + "resource-not-found"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Resource Conflict");
        pd.setType(URI.create(BASE_URI + "resource-conflict"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    /**
     * Handles method-security denials (@PreAuthorize) — a DIFFERENT code path
     * from authorizeHttpRequests' filter-chain denials, which SecurityConfig
     * already routes through RestAccessDeniedHandler. AccessDeniedException
     * thrown by a @PreAuthorize method interceptor surfaces here instead,
     * inside normal Spring MVC exception resolution — without this handler
     * it fell through to the generic 500 handler below (discovered via the
     * first @PreAuthorize check on a boolean flag rather than a role, but it
     * affected every existing hasRole(...)/hasAuthority(...) check too, e.g.
     * UsersController's ADMIN-gated endpoints).
     * Same response shape as RestAccessDeniedHandler for consistency.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
        pd.setTitle("Access Denied");
        pd.setType(URI.create(BASE_URI + "access-denied"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(ExpiredTokenException.class)
    public ProblemDetail handleExpiredToken(ExpiredTokenException ex) {
        log.warn("Expired token: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Token Expired");
        pd.setType(URI.create(BASE_URI + "token-expired"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ProblemDetail handleWeakPassword(WeakPasswordException ex) {
        log.warn("Weak password attempt: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Weak Password");
        pd.setType(URI.create(BASE_URI + "weak-password"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(AccountNotVerifiedException.class)
    public ProblemDetail handleAccountNotVerified(AccountNotVerifiedException ex) {
        log.warn("Unverified account login attempt: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Account Not Verified");
        pd.setType(URI.create(BASE_URI + "account-not-verified"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ProblemDetail handleAccountSuspended(AccountSuspendedException ex) {
        log.warn("Suspended account login attempt: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Account Suspended");
        pd.setType(URI.create(BASE_URI + "account-suspended"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(TenantAccessException.class)
    public ProblemDetail handleTenantAccess(TenantAccessException ex) {
        log.warn("Tenant access violation: {}", ex.getMessage());
        // ex.getMessage() carries the specific stalk/org ids for the log line
        // above, but never reaches the response body — a cross-tenant access
        // attempt should look like "doesn't exist" to the caller, not confirm
        // the resource is real but owned by someone else. Same reasoning as
        // handleAccessDenied's hardcoded message below.
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Resource not found");
        pd.setTitle("Tenant Access Denied");
        pd.setType(URI.create(BASE_URI + "tenant-access-denied"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(DuplicateStalkUrlException.class)
    public ProblemDetail handleDuplicateStalkUrl(DuplicateStalkUrlException ex) {
        log.warn("Duplicate stalk URL rejected: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Duplicate Stalk URL");
        pd.setType(URI.create(BASE_URI + "duplicate-url"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(StalkQuotaExceededException.class)
    public ProblemDetail handleStalkQuotaExceeded(StalkQuotaExceededException ex) {
        log.warn("Stalk quota exceeded: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Stalk Quota Exceeded");
        pd.setType(URI.create(BASE_URI + "stalk-quota-exceeded"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(UnsafeUrlException.class)
    public ProblemDetail handleUnsafeUrl(UnsafeUrlException ex) {
        log.warn("Unsafe URL rejected: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Unsafe URL");
        pd.setType(URI.create(BASE_URI + "unsafe-url"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    /**
     * Not exercised by Bucket4jRateLimitFilter today — that filter writes its
     * 429 directly since it runs outside this class's reach (filters sit
     * outside Spring MVC's exception-resolution machinery). Ready here so a
     * future service-layer rate limit doesn't need a separate handler added
     * later.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ProblemDetail handleTooManyRequests(TooManyRequestsException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Rate Limit Exceeded");
        pd.setType(URI.create(BASE_URI + "rate-limit-exceeded"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    /**
     * Any permitAll'd-but-otherwise-unmapped path (e.g. actuator endpoints not
     * exposed on this port) falls through Spring MVC's handler resolution to
     * the static-resource handler, which throws this instead of a plain 404.
     * Without this handler it fell through to handleInternalError below,
     * turning a routine "nothing here" into a misleading 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "The resource you requested doesn't exist.");
        pd.setTitle("Not Found");
        pd.setType(URI.create(BASE_URI + "not-found"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleInternalError(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Please contact support.");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create(BASE_URI + "internal-error"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        log.warn("Failed login attempt");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Invalid email or password");
        pd.setTitle("Authentication Failed");
        pd.setType(URI.create(BASE_URI + "authentication-failed"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Account is not active. Please verify your email.");
        pd.setTitle("Account Disabled");
        pd.setType(URI.create(BASE_URI + "account-disabled"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLocked(LockedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Account is locked. Contact support.");
        pd.setTitle("Account Locked");
        pd.setType(URI.create(BASE_URI + "account-locked"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}