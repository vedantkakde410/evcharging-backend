package com.evcharging.evcharging.service;

import com.evcharging.evcharging.dto.AuthResponse;
import com.evcharging.evcharging.dto.RegisterResponse;
import com.evcharging.evcharging.dto.ResendOtpResponse;
import com.evcharging.evcharging.entity.EmailVerificationOtp;
import com.evcharging.evcharging.entity.Role;
import com.evcharging.evcharging.entity.User;
import com.evcharging.evcharging.exception.EmailTakenException;
import com.evcharging.evcharging.exception.OtpExpiredException;
import com.evcharging.evcharging.exception.OtpInvalidException;
import com.evcharging.evcharging.exception.TooManyAttemptsException;
import com.evcharging.evcharging.repository.EmailVerificationOtpRepository;
import com.evcharging.evcharging.repository.UserRepository;
import com.evcharging.evcharging.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Orchestrates register -> OTP -> account creation
// (AUTHENTICATION_DESIGN.md section 3.1). The account does not exist in
// `users` until verifyOtp succeeds.
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    public RegistrationService(UserRepository userRepository,
                                EmailVerificationOtpRepository otpRepository,
                                PasswordEncoder passwordEncoder,
                                OtpService otpService,
                                EmailService emailService,
                                JwtService jwtService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse startRegistration(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailTakenException();
        }

        String passwordHash = passwordEncoder.encode(password);
        String otp = otpService.generateOtp();
        String otpHash = otpService.hash(otp);
        Instant expiresAt = Instant.now().plusMillis(OtpService.OTP_TTL_MS);

        // A fresh registration attempt replaces any previous unconsumed one
        // for this email rather than accumulating rows.
        otpRepository.deleteByEmailAndConsumedFalse(email);

        EmailVerificationOtp pending = new EmailVerificationOtp(
                name, email, passwordHash, role, otpHash, expiresAt,
                OtpService.MAX_ATTEMPTS, OtpService.MAX_RESENDS);
        otpRepository.save(pending);

        emailService.sendOtpEmail(email, otp, OtpPurpose.REGISTRATION);

        return new RegisterResponse(email, OtpService.OTP_TTL_MS / 1000);
    }

    // noRollbackFor is required: OtpInvalidException is thrown deliberately
    // to signal failure to the controller, but @Transactional's default
    // behavior rolls back every DB write made in the method for any
    // RuntimeException - without this, the incrementAttempts() write below
    // would be silently undone on every wrong guess, and the 5-attempt
    // lockout would never actually engage. Confirmed live: attemptsRemaining
    // stayed stuck at 4 across repeated wrong OTPs until this was added.
    @Transactional(noRollbackFor = OtpInvalidException.class)
    public AuthResponse verifyOtp(String email, String otp) {
        EmailVerificationOtp pending = otpRepository
                .findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(OtpExpiredException::new);

        if (otpService.isExpired(pending.getExpiresAt())) {
            throw new OtpExpiredException();
        }
        if (pending.getAttempts() >= pending.getMaxAttempts()) {
            throw new TooManyAttemptsException();
        }
        if (!otpService.matches(otp, pending.getOtpHash())) {
            pending.incrementAttempts();
            otpRepository.save(pending);
            throw new OtpInvalidException(pending.getMaxAttempts() - pending.getAttempts());
        }

        pending.markConsumed();
        otpRepository.save(pending);

        User user = new User(pending.getName(), pending.getEmail(), pending.getPasswordHash(),
                pending.getRole(), true);
        user = userRepository.save(user);

        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(token, user.getId(), user.getName(), user.getRole().name(), true);
    }

    @Transactional
    public ResendOtpResponse resendOtp(String email) {
        EmailVerificationOtp pending = otpRepository
                .findTopByEmailAndConsumedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(OtpExpiredException::new);

        otpService.assertCanResend(pending.getLastSentAt(), pending.getResendCount(), pending.getMaxResends());

        String otp = otpService.generateOtp();
        pending.setOtpHash(otpService.hash(otp));
        pending.setExpiresAt(Instant.now().plusMillis(OtpService.OTP_TTL_MS));
        pending.setLastSentAt(Instant.now());
        pending.incrementResendCount();
        otpRepository.save(pending);

        emailService.sendOtpEmail(email, otp, OtpPurpose.REGISTRATION);

        return new ResendOtpResponse(OtpService.OTP_TTL_MS / 1000);
    }
}
