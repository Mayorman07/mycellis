package com.mycelis.shared.exception;

public class AccountNotVerifiedException extends MycelisException {
    public AccountNotVerifiedException(String email) {
        super("Account not verified for: " + email);
    }
}