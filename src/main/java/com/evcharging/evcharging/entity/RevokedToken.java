package com.evcharging.evcharging.entity;

import jakarta.persistence.*;

import java.time.Instant;

// One row per logged-out access token, keyed by its jti claim - see
// AUTHENTICATION_DESIGN.md section 7 (updated by Module 9: logout now
// actually revokes the token server-side instead of only clearing it
// client-side). Rows past their own expires_at are cleanup-eligible since
// the JWT's own expiry check would reject them anyway by then.
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(length = 36)
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        revokedAt = Instant.now();
    }

    public String getJti() {
        return jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
