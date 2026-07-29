package com.evcharging.evcharging.exception;

public class ResendTooSoonException extends RuntimeException {

    private final long retryAfterSeconds;

    public ResendTooSoonException(long retryAfterSeconds) {
        super("Please wait before requesting another code.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
