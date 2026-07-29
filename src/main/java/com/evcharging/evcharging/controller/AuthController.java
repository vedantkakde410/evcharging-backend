package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.dto.*;
import com.evcharging.evcharging.entity.Role;
import com.evcharging.evcharging.entity.User;
import com.evcharging.evcharging.security.TokenRevocationService;
import com.evcharging.evcharging.service.PasswordResetService;
import com.evcharging.evcharging.service.RegistrationService;
import com.evcharging.evcharging.service.UserService;
import com.evcharging.evcharging.service.ValidationUtil;
import com.evcharging.evcharging.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final JwtService jwtService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(RegistrationService registrationService,
                           PasswordResetService passwordResetService,
                           UserService userService,
                           JwtService jwtService,
                           TokenRevocationService tokenRevocationService) {
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        ValidationUtil.assertValidRegistration(
                request.name, request.email, request.password, request.confirmPassword, request.role);

        RegisterResponse response = registrationService.startRegistration(
                request.name, request.email, request.password, Role.valueOf(request.role));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        ValidationUtil.assertValidEmail(request.email);
        ValidationUtil.assertValidOtp(request.otp);

        AuthResponse response = registrationService.verifyOtp(request.email, request.otp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ResendOtpResponse> resendOtp(@RequestBody ResendOtpRequest request) {
        ValidationUtil.assertValidEmail(request.email);

        ResendOtpResponse response = registrationService.resendOtp(request.email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email, request.password);
        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        AuthResponse response = new AuthResponse(
                token, user.getId(), user.getName(), user.getRole().name(), user.isEmailVerified());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        ValidationUtil.assertValidEmail(request.email);

        MessageResponse response = passwordResetService.requestReset(request.email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ResetTokenResponse> verifyResetOtp(@RequestBody VerifyResetOtpRequest request) {
        ValidationUtil.assertValidEmail(request.email);
        ValidationUtil.assertValidOtp(request.otp);

        ResetTokenResponse response = passwordResetService.verifyResetOtp(request.email, request.otp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<ResendOtpResponse> resendResetOtp(@RequestBody ForgotPasswordRequest request) {
        ValidationUtil.assertValidEmail(request.email);

        ResendOtpResponse response = passwordResetService.resendResetOtp(request.email);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        ValidationUtil.assertValidNewPassword(request.newPassword, request.confirmPassword);

        MessageResponse response =
                passwordResetService.resetPassword(request.resetToken, request.newPassword);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Requires a valid, currently-unrevoked access token (see SecurityConfig:
    // this path is authenticated(), not permitAll like the rest of /auth/**).
    // Revokes that specific token's jti so it can no longer be used, even
    // though it hasn't naturally expired yet (AUTHENTICATION_DESIGN.md
    // section 7, updated by Module 9).
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            tokenRevocationService.revoke(jwtService.parseClaims(token));
        }

        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }
}
