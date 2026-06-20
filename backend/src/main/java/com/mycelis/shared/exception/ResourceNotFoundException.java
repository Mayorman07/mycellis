package com.mycelis.shared.exception;

public class ResourceNotFoundException extends MycelisException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}