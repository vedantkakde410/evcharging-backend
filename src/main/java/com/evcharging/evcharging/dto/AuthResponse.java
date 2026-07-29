package com.evcharging.evcharging.dto;

// Shared success shape for /auth/login and /auth/verify-otp - both produce a
// freshly-authenticated session (AUTHENTICATION_DESIGN.md section 4).
public class AuthResponse {

    public String token;
    public Long userId;
    public String name;
    public String role;
    public boolean emailVerified;

    public AuthResponse(String token, Long userId, String name, String role, boolean emailVerified) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.emailVerified = emailVerified;
    }
}
