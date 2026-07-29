package com.evcharging.evcharging.service;

public enum OtpPurpose {

    REGISTRATION("Verify your email — EVCharge", "Verify your email"),
    PASSWORD_RESET("Reset your password — EVCharge", "Reset your password");

    private final String subject;
    private final String heading;

    OtpPurpose(String subject, String heading) {
        this.subject = subject;
        this.heading = heading;
    }

    public String subject() {
        return subject;
    }

    public String heading() {
        return heading;
    }
}
