package com.evcharging.evcharging.exception;

public class OtpInvalidException extends RuntimeException {

    private final int attemptsRemaining;

    public OtpInvalidException(int attemptsRemaining) {
        super("Incorrect code.");
        this.attemptsRemaining = attemptsRemaining;
    }

    public int getAttemptsRemaining() {
        return attemptsRemaining;
    }
}
