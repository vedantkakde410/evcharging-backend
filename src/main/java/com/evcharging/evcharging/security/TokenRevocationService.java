package com.evcharging.evcharging.security;

import com.evcharging.evcharging.entity.RevokedToken;
import com.evcharging.evcharging.repository.RevokedTokenRepository;
import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

// Backs /auth/logout (revoke) and JwtAuthenticationFilter (isRevoked) - see
// AUTHENTICATION_DESIGN.md section 7. A JWT is otherwise stateless, so this
// table is the one piece of server-side session state the auth system has;
// kept small deliberately (jti + expiry only) and self-cleaning via the
// scheduled purge below.
@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void revoke(Claims claims) {
        String jti = claims.getId();
        Date expiration = claims.getExpiration();

        if (jti == null || expiration == null) {
            return;
        }

        revokedTokenRepository.save(new RevokedToken(jti, expiration.toInstant()));
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsById(jti);
    }

    // Hourly: a revoked row past its own expires_at adds nothing (the JWT's
    // own expiry check in JwtAuthenticationFilter would already reject it),
    // so it's safe to purge - keeps this table from growing unbounded.
    @Scheduled(cron = "0 0 * * * *")
    public void purgeExpired() {
        revokedTokenRepository.deleteAllByExpiresAtBefore(Instant.now());
    }
}
