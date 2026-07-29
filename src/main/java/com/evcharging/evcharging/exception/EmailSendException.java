package com.evcharging.evcharging.exception;

public class EmailSendException extends RuntimeException {
    public EmailSendException(Throwable cause) {
        super("Could not send the verification email. Please try again shortly.", cause);
    }
}
