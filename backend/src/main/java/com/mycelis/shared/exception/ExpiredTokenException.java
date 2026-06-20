package com.mycelis.shared.exception;

public class ExpiredTokenException extends MycelisException {
    public ExpiredTokenException(String tokenType) {
        super(tokenType + " token has expired");
    }
}