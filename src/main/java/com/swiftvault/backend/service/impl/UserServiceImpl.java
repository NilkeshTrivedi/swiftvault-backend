package com.swiftvault.backend.service.impl;

import com.swiftvault.backend.dto.request.*;
import com.swiftvault.backend.dto.response.AuthResponse;
import com.swiftvault.backend.dto.response.UserResponse;
import com.swiftvault.backend.entity.User;
import com.swiftvault.backend.exception.SwiftVaultException;
import com.swiftvault.backend.repository.UserRepository;
import com.swiftvault.backend.security.JwtUtil;
import com.swiftvault.backend.service.UserService;
import com.swiftvault.backend.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserServiceImpl — full implementation of all user operations.
 *
 * @Slf4j      → Lombok: generates a 'log' field for logging
 * @Service    → Spring: registers as a service bean
 * @Transactional → wraps methods in DB transactions automatically
 *               If any exception occurs, the entire operation rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int    MAX_FAILED_ATTEMPTS = 3;
    private static final int    LOCKOUT_MINUTES     = 30;

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    // ─── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw SwiftVaultException.conflict("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .userId(IdGenerator.userId())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(token, user);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> SwiftVaultException.notFound(
                        "No account found for: " + request.getEmail()));

        // Check lock status (auto-unlocks if lockout period expired)
        if (user.isAccountLocked()) {
            userRepository.save(user); // persist auto-unlock if applicable
            if (user.isAccountLocked()) {
                throw SwiftVaultException.unauthorized(
                        "Account is locked. Try again after " + LOCKOUT_MINUTES + " minutes.");
            }
        }

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            throw SwiftVaultException.forbidden("Account is suspended. Contact support.");
        }

        // Wrong password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            int remaining = MAX_FAILED_ATTEMPTS - user.getFailedAttempts();

            if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setStatus(User.UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
                userRepository.save(user);
                log.warn("Account locked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, user.getEmail());
                throw SwiftVaultException.unauthorized(
                        "Too many failed attempts. Account locked for " + LOCKOUT_MINUTES + " minutes.");
            }

            userRepository.save(user);
            throw SwiftVaultException.unauthorized(
                    "Incorrect password. " + remaining + " attempt(s) remaining.");
        }

        // Success — reset counters
        user.setFailedAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        log.info("User logged in: {}", user.getEmail());

        String token = jwtUtil.generateToken(user);
        return buildAuthResponse(token, user);
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @Override
    public UserResponse getProfile(String userId) {
        return UserResponse.from(findById(userId));
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw SwiftVaultException.badRequest("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for: {}", user.getEmail());
    }

    // ─── Transaction PIN ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public void setTransactionPin(String userId, SetPinRequest request) {
        if (!request.getPin().equals(request.getConfirmPin())) {
            throw SwiftVaultException.badRequest("PINs do not match.");
        }

        User user = findById(userId);
        user.setPinHash(passwordEncoder.encode(request.getPin()));
        userRepository.save(user);
        log.info("Transaction PIN set for: {}", user.getEmail());
    }

    @Override
    public void verifyTransactionPin(String userId, String pin) {
        User user = findById(userId);

        if (!user.hasTransactionPin()) {
            throw SwiftVaultException.badRequest(
                    "No transaction PIN set. Please set a PIN first.");
        }

        if (!passwordEncoder.matches(pin, user.getPinHash())) {
            throw SwiftVaultException.unauthorized("Incorrect transaction PIN.");
        }
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> searchUsers(String query) {
        return userRepository.searchUsers(query).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void suspendUser(String userId) {
        User user = findById(userId);
        user.setStatus(User.UserStatus.SUSPENDED);
        userRepository.save(user);
        log.info("User suspended: {}", userId);
    }

    @Override
    @Transactional
    public void activateUser(String userId) {
        User user = findById(userId);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("User activated: {}", userId);
    }

    @Override
    @Transactional
    public void adminResetPassword(String targetUserId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw SwiftVaultException.badRequest("Password must be at least 6 characters.");
        }
        User user = findById(targetUserId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Admin reset password for: {}", targetUserId);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @Override
    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> SwiftVaultException.notFound("User not found: " + userId));
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .hasTransactionPin(user.hasTransactionPin())
                .build();
    }
}