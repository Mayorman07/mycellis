package com.mycelis.shared.exception;

/**
 * Thrown when a user attempts to log in with valid credentials, but their account
 * has not yet been email-verified (Status.NEW).
 *
 * <p>This is distinct from {@code BadCredentialsException} — the credentials WERE
 * correct, just the account isn't activated yet. Maps to a 403 with a structured
 * response telling the client to prompt the user to check their email or resend
 * the verification link.</p>
 */
public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException(String message) {
        super(message);
    }

    public EmailNotVerifiedException() {
        super("Email not verified. Please check your inbox for a verification link.");
    }
}