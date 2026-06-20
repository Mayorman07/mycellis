package com.mycelis.shared.exception;

public class AccountSuspendedException extends MycelisException {
    public AccountSuspendedException(String email) {
        super("Account suspended for: " + email);
    }
}