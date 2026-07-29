package com.evcharging.evcharging.dto;

public class ResendOtpResponse {

    public long expiresInSeconds;

    public ResendOtpResponse(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
