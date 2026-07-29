package com.evcharging.evcharging.exception;

public class InvalidResetTokenException extends RuntimeException {
    public InvalidResetTokenException() {
        super("This password reset session is invalid or has expired. Start over.");
    }
}
