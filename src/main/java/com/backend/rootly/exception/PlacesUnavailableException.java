package com.backend.rootly.exception;

import java.time.Duration;

public class PlacesUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final Duration retryDelay;

    public PlacesUnavailableException(String message, Duration retryAfter) {
        super(message);
        this.retryDelay = retryAfter;
    }

    public PlacesUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.retryDelay = Duration.ofSeconds(60);
    }

    public Duration getRetryAfter() {
        return retryDelay;
    }
}
