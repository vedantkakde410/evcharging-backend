package com.evcharging.evcharging.controller;

import com.evcharging.evcharging.dto.ApiErrorResponse;
import com.evcharging.evcharging.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Scoped to AuthController only - every other controller keeps its existing
// (inconsistent) contract, per AUTHENTICATION_DESIGN.md section 13, Risk 1:
// this is a deliberate, narrow exception to "never redesign backend APIs",
// not a project-wide error-contract change.
@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("VALIDATION_ERROR", e.getMessage()).withFields(e.getFields()));
    }

    @ExceptionHandler(EmailTakenException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailTaken(EmailTakenException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("EMAIL_TAKEN", e.getMessage()));
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpInvalid(OtpInvalidException e) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse("OTP_INVALID", e.getMessage())
                        .withAttemptsRemaining(e.getAttemptsRemaining()));
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpExpired(OtpExpiredException e) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ApiErrorResponse("OTP_EXPIRED", e.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyAttempts(TooManyAttemptsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("TOO_MANY_ATTEMPTS", e.getMessage()));
    }

    @ExceptionHandler(ResendLimitReachedException.class)
    public ResponseEntity<ApiErrorResponse> handleResendLimit(ResendLimitReachedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("RESEND_LIMIT_REACHED", e.getMessage()));
    }

    @ExceptionHandler(ResendTooSoonException.class)
    public ResponseEntity<ApiErrorResponse> handleResendTooSoon(ResendTooSoonException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse("RESEND_TOO_SOON", e.getMessage())
                        .withRetryAfterSeconds(e.getRetryAfterSeconds()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("INVALID_CREDENTIALS", e.getMessage()));
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidResetToken(InvalidResetTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse("RESET_TOKEN_INVALID", e.getMessage()));
    }

    @ExceptionHandler(EmailSendException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailSendFailure(EmailSendException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse("EMAIL_SEND_FAILED", e.getMessage()));
    }
}
