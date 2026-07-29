package com.evcharging.evcharging.repository;

import com.evcharging.evcharging.entity.EmailVerificationOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {
    Optional<EmailVerificationOtp> findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(String email);
    void deleteByEmailAndConsumedFalse(String email);
}
