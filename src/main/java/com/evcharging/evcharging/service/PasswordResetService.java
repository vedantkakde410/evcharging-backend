package com.evcharging.evcharging.service;

import com.evcharging.evcharging.dto.MessageResponse;
import com.evcharging.evcharging.dto.ResendOtpResponse;
import com.evcharging.evcharging.dto.ResetTokenResponse;
import com.evcharging.evcharging.entity.PasswordResetOtp;
import com.evcharging.evcharging.entity.User;
import com.evcharging.evcharging.exception.OtpExpiredException;
import com.evcharging.evcharging.exception.OtpInvalidException;
import com.evcharging.evcharging.exception.TooManyAttemptsException;
import com.evcharging.evcharging.repository.PasswordResetOtpRepository;
import com.evcharging.evcharging.repository.UserRepository;
import com.evcharging.evcharging.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

// Orchestrates forgot-password -> OTP -> reset-token -> new password
// (AUTHENTICATION_DESIGN.md section 3.3).
@Service
public class PasswordResetService {

    private static final String GENERIC_REQUEST_MESSAGE =
            "If that email is registered, a code was sent.";

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final JwtService jwtService;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordResetOtpRepository otpRepository,
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

    // Deliberately returns the same message whether or not the email exists,
    // to avoid using this endpoint to enumerate registered accounts - see
    // AUTHENTICATION_DESIGN.md section 13, Risk 2.
    @Transactional
    public MessageResponse requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String otp = otpService.generateOtp();
            String otpHash = otpService.hash(otp);
            Instant expiresAt = Instant.now().plusMillis(OtpService.OTP_TTL_MS);

            otpRepository.deleteByUserIdAndConsumedFalse(user.getId());

            PasswordResetOtp pending = new PasswordResetOtp(
                    user.getId(), otpHash, expiresAt, OtpService.MAX_ATTEMPTS, OtpService.MAX_RESENDS);
            otpRepository.save(pending);

            emailService.sendOtpEmail(email, otp, OtpPurpose.PASSWORD_RESET);
        }

        return new MessageResponse(GENERIC_REQUEST_MESSAGE);
    }

    // See RegistrationService.verifyOtp's comment - same rollback-vs-attempt-
    // counter bug, same fix.
    @Transactional(noRollbackFor = OtpInvalidException.class)
    public ResetTokenResponse verifyResetOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(OtpExpiredException::new);

        PasswordResetOtp pending = otpRepository
                .findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(user.getId())
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

        String resetToken = jwtService.generateResetToken(user.getId());
        return new ResetTokenResponse(resetToken);
    }

    @Transactional
    public ResendOtpResponse resendResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(OtpExpiredException::new);

        PasswordResetOtp pending = otpRepository
                .findTopByUserIdAndConsumedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(OtpExpiredException::new);

        otpService.assertCanResend(pending.getLastSentAt(), pending.getResendCount(), pending.getMaxResends());

        String otp = otpService.generateOtp();
        pending.setOtpHash(otpService.hash(otp));
        pending.setExpiresAt(Instant.now().plusMillis(OtpService.OTP_TTL_MS));
        pending.setLastSentAt(Instant.now());
        pending.incrementResendCount();
        otpRepository.save(pending);

        emailService.sendOtpEmail(email, otp, OtpPurpose.PASSWORD_RESET);

        return new ResendOtpResponse(OtpService.OTP_TTL_MS / 1000);
    }

    @Transactional
    public MessageResponse resetPassword(String resetToken, String newPassword) {
        Long userId = jwtService.extractResetUserId(resetToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Reset token references a user that no longer exists"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // The OTP behind this reset token was already marked consumed in
        // verifyResetOtp; nothing further to invalidate for it here. The
        // reset token itself has no server-side revocation (short 10-minute
        // expiry is the mitigation - see AUTHENTICATION_DESIGN.md section 7).

        return new MessageResponse("Password updated");
    }
}
