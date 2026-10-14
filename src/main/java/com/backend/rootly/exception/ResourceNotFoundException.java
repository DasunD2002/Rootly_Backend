package com.backend.rootly.exception;

/** Indicates that an API request references a resource that does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
