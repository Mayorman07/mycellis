package com.mycelis.shared.exception;

public abstract class MycelisException extends RuntimeException {
    protected MycelisException(String message) {
        super(message);
    }
}