package com.mycelis.shared.exception;

public class WeakPasswordException extends MycelisException {

    private WeakPasswordException(String message) {
        super(message);
    }

    public static WeakPasswordException tooShort(int minLength) {
        return new WeakPasswordException("Password must be at least " + minLength + " characters");
    }

    public static WeakPasswordException requiresUppercase() {
        return new WeakPasswordException("Password must contain at least one uppercase letter");
    }

    public static WeakPasswordException requiresLowercase() {
        return new WeakPasswordException("Password must contain at least one lowercase letter");
    }

    public static WeakPasswordException requiresNumber() {
        return new WeakPasswordException("Password must contain at least one number");
    }

    public static WeakPasswordException requiresSpecialChar() {
        return new WeakPasswordException("Password must contain at least one special character");
    }
}
