package com.app.auth.service.impl;

import com.app.auth.dto.request.LoginRequestDTO;
import com.app.auth.dto.response.AuthResponseDTO;
import com.app.auth.exception.*;
import com.app.auth.model.*;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.*;
import com.app.auth.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TechnicianProfileService technicianProfileService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private long accountLockDurationMinutes;

    @Value("${app.security.email-verification-token-expiry-hours:24}")
    private long emailVerificationTokenExpiryHours;

    @Value("${app.security.password-reset-token-expiry-hours:1}")
    private long passwordResetTokenExpiryHours;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request, HttpServletRequest httpRequest) {

        String ipAddress = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        // Rate limiting
        rateLimitService.checkLoginRateLimit(request.getUsername(), ipAddress);

        // Find user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    auditLogService.logFailedLogin(
                            request.getUsername(),
                            "User not found",
                            ipAddress,
                            userAgent
                    );
                    return new InvalidCredentialsException("Invalid username or password");
                });

        // Check if account is locked
        if (user.isLocked()) {
            auditLogService.logFailedLogin(
                    user.getUsername(),
                    "Account locked",
                    ipAddress,
                    userAgent
            );
            throw new AccountLockedException(
                    "Account is temporarily locked due to multiple failed login attempts",
                    user.getLockedUntil()
            );
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Check if email is verified
        if (!user.isEmailVerified()) {
            auditLogService.logFailedLogin(
                    user.getUsername(),
                    "Email not verified",
                    ipAddress,
                    userAgent
            );
            throw new EmailNotVerifiedException(
                    "Please verify your email before logging in. Check your inbox for verification link."
            );
        }

        // Check 2FA if enabled
        if (user.isTwoFactorEnabled() &&
                (request.getTwoFactorCode() == null || request.getTwoFactorCode().isEmpty())) {

            String tempToken = jwtUtils.generateTempToken(user.getUsername());
            throw new TwoFactorRequiredException(
                    "Two-factor authentication required",
                    tempToken
            );
        }

        // Reset failed attempts on successful login
        user.resetFailedAttempts();
        user.setLastLoginIp(ipAddress);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                ipAddress,
                userAgent
        );

        // Audit log
        auditLogService.logSuccessfulLogin(user.getId(), user.getUsername(), ipAddress, userAgent);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponseDTO registerCustomer(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            String city,
            String state,
            String zipCode
    ) {
        validateUserDoesNotExist(username, email);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(false)
                .build();

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(
                Instant.now().plusSeconds(emailVerificationTokenExpiryHours * 3600)
        );

        user = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(email, username, verificationToken);

        // Audit log
        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.REGISTER,
                "Customer registered",
                null,
                null,
                true,
                null
        );

        // Generate tokens (they can use app but should verify email)
        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                null,
                null
        );

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public AuthResponseDTO registerTechnician(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String phoneNumber,
            Set<String> skills,
            Integer experienceYears,
            String bio,
            String city,
            String state,
            String idProofType
    ) {
        validateUserDoesNotExist(username, email);

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .city(city)
                .state(state)
                .roles(Set.of(UserRole.ROLE_TECHNICIAN))
                .enabled(true)
                .emailVerified(false)
                .build();

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(
                Instant.now().plusSeconds(emailVerificationTokenExpiryHours * 3600)
        );

        user = userRepository.save(user);

        // Create technician profile (pending approval)
        technicianProfileService.createProfile(
                user.getId(),
                skills,
                experienceYears,
                bio,
                city,
                state,
                idProofType
        );

        // Send verification email
        emailService.sendVerificationEmail(email, username, verificationToken);

        // Audit log
        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.REGISTER,
                "Technician registered - pending approval",
                null,
                null,
                true,
                null
        );

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getRoles());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                user.getId(),
                null,
                null
        );

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiry().isBefore(Instant.now())) {
            throw new TokenExpiredException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.EMAIL_VERIFIED,
                "Email verified successfully",
                null,
                null,
                true,
                null
        );

        log.info("Email verified for user: {}", user.getUsername());
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        // Generate new token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(
                Instant.now().plusSeconds(emailVerificationTokenExpiryHours * 3600)
        );
        userRepository.save(user);

        emailService.sendVerificationEmail(email, user.getUsername(), verificationToken);
    }

    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this email"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(
                Instant.now().plusSeconds(passwordResetTokenExpiryHours * 3600)
        );
        userRepository.save(user);

        emailService.sendPasswordResetEmail(email, user.getUsername(), resetToken);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.PASSWORD_RESET_REQUESTED,
                "Password reset initiated",
                null,
                null,
                true,
                null
        );
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.resetFailedAttempts(); // Reset any failed attempts
        userRepository.save(user);

        // Revoke all existing refresh tokens for security
        refreshTokenService.revokeAllUserTokens(user.getId(), "Password reset");

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.PASSWORD_RESET_COMPLETED,
                "Password reset completed",
                null,
                null,
                true,
                null
        );
    }

    @Override
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.PASSWORD_CHANGED,
                "Password changed successfully",
                null,
                null,
                true,
                null
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken, HttpServletRequest httpRequest) {
        String ipAddress = RequestUtils.getClientIp(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        RefreshToken token = refreshTokenService.findByToken(refreshToken);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        refreshTokenService.revokeToken(refreshToken, "User logout");

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.LOGOUT,
                "User logged out",
                ipAddress,
                userAgent,
                true,
                null
        );
    }

    // ===============================
    // Helper methods
    // ===============================

    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.lockAccount(accountLockDurationMinutes);
            log.warn("Account locked for user: {} after {} failed attempts",
                    user.getUsername(), maxLoginAttempts);

            auditLogService.log(
                    user.getId(),
                    user.getUsername(),
                    AuditLog.AuditAction.ACCOUNT_LOCKED,
                    "Account locked after " + maxLoginAttempts + " failed login attempts",
                    ipAddress,
                    userAgent,
                    false,
                    "Too many failed login attempts"
            );
        }

        userRepository.save(user);

        auditLogService.logFailedLogin(
                user.getUsername(),
                "Invalid password",
                ipAddress,
                userAgent
        );
    }

    private void validateUserDoesNotExist(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already exists");
        }
    }

    private AuthResponseDTO buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getAccessTokenExpiration() / 1000)
                .user(AuthResponseDTO.UserInfoDTO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .roles(user.getRoles())
                        .emailVerified(user.isEmailVerified())
                        .twoFactorEnabled(user.isTwoFactorEnabled())
                        .build())
                .build();
    }
}