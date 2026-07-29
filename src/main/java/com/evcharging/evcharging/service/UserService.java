package com.evcharging.evcharging.service;

import com.evcharging.evcharging.entity.User;
import com.evcharging.evcharging.exception.InvalidCredentialsException;
import com.evcharging.evcharging.exception.InvalidResetTokenException;
import com.evcharging.evcharging.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Same exception regardless of whether the email exists or the password
    // is wrong - never lets login distinguish the two (AUTHENTICATION_DESIGN.md
    // section 3.2).
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return user;
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(InvalidResetTokenException::new);
    }

    public void updatePassword(User user, String newPasswordHash) {
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }
}
