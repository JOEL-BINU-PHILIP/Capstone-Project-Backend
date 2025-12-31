package com.app.auth.service;

public interface EmailService {
    void sendVerificationEmail(String email, String username, String token);
    void sendPasswordResetEmail(String email, String username, String token);
    void sendWelcomeEmail(String email, String username);
}