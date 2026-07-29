package com.evcharging.evcharging.repository;

import com.evcharging.evcharging.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    void deleteAllByExpiresAtBefore(Instant cutoff);
}
