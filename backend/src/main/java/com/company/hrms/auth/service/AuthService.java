package com.company.hrms.auth.service;

import com.company.hrms.auth.dto.*;
import com.company.hrms.auth.entity.*;
import com.company.hrms.auth.repository.*;
import com.company.hrms.auth.security.*;
import com.company.hrms.common.exception.BusinessException;
import com.company.hrms.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager   authManager;
    private final UserRepository          userRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final EmployeeRepository      employeeRepository;
    private final JwtService              jwtService;
    private final PasswordEncoder         passwordEncoder;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_MINUTES = 30;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isActive()) throw new BusinessException("Account is deactivated. Contact HR.");
        if (isLocked(user)) throw new BusinessException("Account locked. Try again later.");

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedAttempt(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failed attempts on success
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        var employee = employeeRepository.findById(user.getEmployeeId()).orElseThrow();
        String fullName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");

        return new LoginResponse(
                jwtService.generateAccessToken(userDetails),
                user.getId(),
                user.getEmployeeId(),
                user.getUsername(),
                fullName,
                employee.getEmployeeCode(),
                userDetails.getRoles(),
                user.isMustChangePassword()
        );
    }

    @Transactional
    public String generateRefreshToken(Long userId) {
        String raw = UUID.randomUUID().toString();
        String hash = sha256(raw);
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .build();
        refreshTokenRepository.save(token);
        return raw; // return raw token — only hash is stored in DB
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank())
            throw new BusinessException("No refresh token provided");

        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (!stored.isValid()) throw new BusinessException("Refresh token expired or revoked");

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!user.isActive()) throw new BusinessException("Account is deactivated");

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        var employee = employeeRepository.findById(user.getEmployeeId()).orElseThrow();
        String fullName = employee.getFirstName() + (employee.getLastName() != null ? " " + employee.getLastName() : "");

        return new LoginResponse(
                jwtService.generateAccessToken(userDetails),
                user.getId(), user.getEmployeeId(), user.getUsername(),
                fullName, employee.getEmployeeCode(),
                userDetails.getRoles(), user.isMustChangePassword()
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null) return;
        String hash = sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevokedAt(Instant.now());
            refreshTokenRepository.save(t);
        });
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash()))
            throw new BusinessException("Current password is incorrect");
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        user.setTokenVersion(user.getTokenVersion() + 1); // invalidate existing tokens
        userRepository.save(user);
    }

    private boolean isLocked(User user) {
        return user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil());
    }

    private void handleFailedAttempt(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            log.warn("Account locked for user: {}", user.getUsername());
        }
        userRepository.save(user);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing error", e);
        }
    }
}
