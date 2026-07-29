package com.evcharging.evcharging.security;

import com.evcharging.evcharging.entity.Role;
import com.evcharging.evcharging.exception.InvalidResetTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// Replaces the old static security.JwtUtil - a Spring-managed bean with a
// configurable secret (see application.properties app.jwt.secret) instead of
// a key regenerated randomly on every JVM restart (AUTHENTICATION_DESIGN.md
// section 5).
@Component
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_PURPOSE = "purpose";
    private static final String PURPOSE_PASSWORD_RESET = "password_reset";

    private final Key key;
    private final long accessTokenExpiryMs;
    private final long resetTokenExpiryMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
                       @Value("${app.jwt.reset-token-expiry-ms}") long resetTokenExpiryMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.resetTokenExpiryMs = resetTokenExpiryMs;
    }

    public String generateAccessToken(Long userId, String email, Role role) {
        Instant now = Instant.now();

        // A unique jti per token is what makes logout-revocation possible
        // without invalidating a user's *other* sessions - see
        // TokenRevocationService and JwtAuthenticationFilter.
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_ROLE, role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Single-purpose token exchanged for the OTP after forgot-password
    // verification, so the OTP itself can never be replayed against
    // /auth/reset-password - see AUTHENTICATION_DESIGN.md section 5.
    public String generateResetToken(Long userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_PURPOSE, PURPOSE_PASSWORD_RESET)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(resetTokenExpiryMs)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Throws InvalidResetTokenException for anything that isn't a
    // well-formed, unexpired, purpose=password_reset token - callers don't
    // need to separately check signature/expiry/purpose.
    public Long extractResetUserId(String resetToken) {
        Claims claims;

        try {
            claims = parseClaims(resetToken);
        } catch (Exception e) {
            throw new InvalidResetTokenException();
        }

        if (!PURPOSE_PASSWORD_RESET.equals(claims.get(CLAIM_PURPOSE))) {
            throw new InvalidResetTokenException();
        }

        return Long.valueOf(claims.getSubject());
    }
}
