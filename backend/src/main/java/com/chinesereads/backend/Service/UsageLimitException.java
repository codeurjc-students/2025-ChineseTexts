package com.chinesereads.backend.Service;

/**
 * Thrown when a text generation cannot proceed because a usage limit was reached
 * (per-user monthly quota or the global daily kill-switch). The controller maps
 * it to HTTP 429 with {@link #getMessage()} shown to the user.
 */
public class UsageLimitException extends RuntimeException {
    public UsageLimitException(String message) {
        super(message);
    }
}
