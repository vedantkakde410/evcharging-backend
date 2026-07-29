package com.evcharging.evcharging.exception;

public class OtpExpiredException extends RuntimeException {
    public OtpExpiredException() {
        super("This code has expired. Request a new one.");
    }
}
