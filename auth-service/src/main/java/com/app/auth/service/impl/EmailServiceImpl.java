package com.app.auth.service.impl;

import com.app.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Async
    public void sendVerificationEmail(String email, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Verify Your Email - Home Service Platform");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Thank you for registering! Please verify your email by clicking the link below:\n\n" +
                            "%s/api/auth/verify-email?token=%s\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you didn't create this account, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "Home Service Platform Team",
                    username, baseUrl, token
            ));

            mailSender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String email, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Password Reset Request - Home Service Platform");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "We received a request to reset your password. Click the link below to proceed:\n\n" +
                            "%s/reset-password?token=%s\n\n" +
                            "This link will expire in 1 hour.\n\n" +
                            "If you didn't request this, please ignore this email and your password will remain unchanged.\n\n" +
                            "Best regards,\n" +
                            "Home Service Platform Team",
                    username, baseUrl, token
            ));

            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String email, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Welcome to Home Service Platform!");
            message.setText(String.format(
                    "Hello %s,\n\n" +
                            "Welcome to our Home Service Platform! Your account has been successfully created.\n\n" +
                            "You can now start booking services or, if you're a technician, wait for approval from our service managers.\n\n" +
                            "Best regards,\n" +
                            "Home Service Platform Team",
                    username
            ));

            mailSender.send(message);
            log.info("Welcome email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", email, e);
        }
    }
}