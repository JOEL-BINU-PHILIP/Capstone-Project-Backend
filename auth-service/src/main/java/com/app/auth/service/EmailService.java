package com.app.auth.service;

public interface EmailService {
    void sendVerificationEmail(String email, String username, String token);
    void sendWelcomeEmail(String email, String username);

    // ADD THIS
    void sendTechnicianApprovalEmail(String email, String username);
    void sendTechnicianRejectionEmail(String email, String username, String reason);
}