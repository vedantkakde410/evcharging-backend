package com.evcharging.evcharging.exception;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException() {
        super("Too many incorrect attempts. Request a new code.");
    }
}
