package com.evcharging.evcharging.exception;

public class ResendLimitReachedException extends RuntimeException {
    public ResendLimitReachedException() {
        super("You've reached the maximum number of code resends. Try again later.");
    }
}
