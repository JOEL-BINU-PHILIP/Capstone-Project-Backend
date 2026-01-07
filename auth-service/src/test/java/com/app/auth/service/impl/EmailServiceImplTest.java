package com.app.auth.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @InjectMocks
    private EmailServiceImpl emailService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
    }

    // ==================== SEND VERIFICATION EMAIL TESTS ====================

    @Test
    void sendVerificationEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendVerificationEmail("user@test.com", "testuser", "verification-token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@test.com", message.getFrom());
        assertArrayEquals(new String[]{"user@test.com"}, message.getTo());
        assertEquals("Verify Your Email - Home Service Platform", message.getSubject());
        assertTrue(message.getText().contains("testuser"));
        assertTrue(message.getText().contains("verification-token-123"));
        assertTrue(message.getText().contains("http://localhost:8080"));
    }

    @Test
    void sendVerificationEmail_mailException() {
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception - catches internally
        assertDoesNotThrow(() ->
                emailService.sendVerificationEmail("user@test.com", "testuser", "token")
        );

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ==================== SEND WELCOME EMAIL TESTS ====================

    @Test
    void sendWelcomeEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendWelcomeEmail("user@test.com", "testuser");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@test.com", message.getFrom());
        assertArrayEquals(new String[]{"user@test.com"}, message.getTo());
        assertEquals("Welcome to Home Service Platform!", message.getSubject());
        assertTrue(message.getText().contains("testuser"));
        assertTrue(message.getText().contains("Welcome"));
    }

    @Test
    void sendWelcomeEmail_mailException() {
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception - catches internally
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail("user@test.com", "testuser"));
    }

    // ==================== SEND TECHNICIAN APPROVAL EMAIL TESTS ====================

    @Test
    void sendTechnicianApprovalEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendTechnicianApprovalEmail("tech@test.com", "techuser");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@test.com", message.getFrom());
        assertArrayEquals(new String[]{"tech@test.com"}, message.getTo());
        assertTrue(message.getSubject().contains("Approved"));
        assertTrue(message.getText().contains("techuser"));
        assertTrue(message.getText().contains("approved"));
        assertTrue(message.getText().contains("http://localhost:8080"));
    }

    @Test
    void sendTechnicianApprovalEmail_mailException() {
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception - catches internally
        assertDoesNotThrow(() ->
                emailService.sendTechnicianApprovalEmail("tech@test.com", "techuser")
        );
    }

    // ==================== SEND TECHNICIAN REJECTION EMAIL TESTS ====================

    @Test
    void sendTechnicianRejectionEmail_success() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendTechnicianRejectionEmail("tech@test.com", "techuser", "Invalid documents");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@test.com", message.getFrom());
        assertArrayEquals(new String[]{"tech@test.com"}, message.getTo());
        assertTrue(message.getSubject().contains("Update"));
        assertTrue(message.getText().contains("techuser"));
        assertTrue(message.getText().contains("rejected"));
        assertTrue(message.getText().contains("Invalid documents"));
    }

    @Test
    void sendTechnicianRejectionEmail_mailException() {
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception - catches internally
        assertDoesNotThrow(() ->
                emailService.sendTechnicianRejectionEmail("tech@test.com", "techuser", "Reason")
        );
    }
}

