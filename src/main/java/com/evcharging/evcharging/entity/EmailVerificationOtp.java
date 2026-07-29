package com.evcharging.evcharging.entity;

import jakarta.persistence.*;

import java.time.Instant;

// Holds a *pending* registration - the user row does not exist until the OTP
// stored here is verified (see AUTHENTICATION_DESIGN.md section 2.2).
@Entity
@Table(name = "email_verification_otp")
public class EmailVerificationOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "max_resends", nullable = false)
    private int maxResends;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailVerificationOtp() {
    }

    public EmailVerificationOtp(String name, String email, String passwordHash, Role role,
                                 String otpHash, Instant expiresAt, int maxAttempts, int maxResends) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.maxAttempts = maxAttempts;
        this.resendCount = 0;
        this.maxResends = maxResends;
        this.lastSentAt = Instant.now();
        this.consumed = false;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getResendCount() {
        return resendCount;
    }

    public void incrementResendCount() {
        this.resendCount++;
    }

    public int getMaxResends() {
        return maxResends;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }

    public void setLastSentAt(Instant lastSentAt) {
        this.lastSentAt = lastSentAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void markConsumed() {
        this.consumed = true;
    }
}
