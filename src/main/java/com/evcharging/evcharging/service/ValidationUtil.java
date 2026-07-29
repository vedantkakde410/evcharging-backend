package com.evcharging.evcharging.service;

import com.evcharging.evcharging.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Server-side validation for registration/reset requests - the real
// boundary. Client-side validation (lib/validation.js) exists purely for
// fast feedback, per ARCHITECTURE_DECISIONS.md section 8.
public final class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // At least 8 characters, one uppercase, one lowercase, one digit.
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private ValidationUtil() {
    }

    public static void assertValidRegistration(String name, String email, String password,
                                                 String confirmPassword, String role) {
        Map<String, String> fields = new HashMap<>();

        if (isBlank(name)) {
            fields.put("name", "Name is required.");
        }
        if (isBlank(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            fields.put("email", "Enter a valid email address.");
        }
        if (isBlank(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            fields.put("password", "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, and a digit.");
        }
        if (isBlank(confirmPassword) || !confirmPassword.equals(password)) {
            fields.put("confirmPassword", "Passwords do not match.");
        }
        if (isBlank(role) || !(role.equals("USER") || role.equals("OWNER"))) {
            fields.put("role", "Select an account type.");
        }

        if (!fields.isEmpty()) {
            throw new ValidationException(fields);
        }
    }

    public static void assertValidNewPassword(String newPassword, String confirmPassword) {
        Map<String, String> fields = new HashMap<>();

        if (isBlank(newPassword) || !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            fields.put("newPassword", "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, and a digit.");
        }
        if (isBlank(confirmPassword) || !confirmPassword.equals(newPassword)) {
            fields.put("confirmPassword", "Passwords do not match.");
        }

        if (!fields.isEmpty()) {
            throw new ValidationException(fields);
        }
    }

    public static void assertValidEmail(String email) {
        if (isBlank(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(Map.of("email", "Enter a valid email address."));
        }
    }

    public static void assertValidOtp(String otp) {
        if (isBlank(otp) || !otp.matches("^\\d{6}$")) {
            throw new ValidationException(Map.of("otp", "Enter the 6-digit code."));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
