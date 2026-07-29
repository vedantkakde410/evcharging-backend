package com.evcharging.evcharging.service;

import com.evcharging.evcharging.exception.ResendLimitReachedException;
import com.evcharging.evcharging.exception.ResendTooSoonException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

// Entity-agnostic OTP primitives shared by both the registration and
// password-reset flows (AUTHENTICATION_DESIGN.md section 5) - callers pass
// their own entity's fields in rather than this service knowing about
// EmailVerificationOtp/PasswordResetOtp directly.
@Service
public class OtpService {

    public static final long OTP_TTL_MS = 5 * 60 * 1000L;
    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_RESENDS = 3;
    public static final long RESEND_COOLDOWN_MS = 60 * 1000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;

    public OtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generateOtp() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    public String hash(String otp) {
        return passwordEncoder.encode(otp);
    }

    public boolean matches(String otp, String otpHash) {
        return passwordEncoder.matches(otp, otpHash);
    }

    public boolean isExpired(Instant expiresAt) {
        return Instant.now().isAfter(expiresAt);
    }

    // Throws if a resend isn't allowed yet - either the per-attempt resend
    // budget is exhausted or the cooldown since the last send hasn't elapsed.
    public void assertCanResend(Instant lastSentAt, int resendCount, int maxResends) {
        if (resendCount >= maxResends) {
            throw new ResendLimitReachedException();
        }

        long elapsedMs = Duration.between(lastSentAt, Instant.now()).toMillis();

        if (elapsedMs < RESEND_COOLDOWN_MS) {
            long retryAfterSeconds = (RESEND_COOLDOWN_MS - elapsedMs + 999) / 1000;
            throw new ResendTooSoonException(retryAfterSeconds);
        }
    }
}
