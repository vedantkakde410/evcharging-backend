package com.evcharging.evcharging.dto;

public class RegisterResponse {

    public String email;
    public long expiresInSeconds;

    public RegisterResponse(String email, long expiresInSeconds) {
        this.email = email;
        this.expiresInSeconds = expiresInSeconds;
    }
}
