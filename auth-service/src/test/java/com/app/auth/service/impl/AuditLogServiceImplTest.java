package com.app.auth.service.impl;

import com.app.auth.model.AuditLog;
import com.app.auth.repository.AuditLogRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testAuditLog = AuditLog.builder()
                .id("log123")
                .userId("user123")
                .username("testuser")
                .action(AuditLog.AuditAction.LOGIN_SUCCESS)
                .details("Successful login")
                .ipAddress("127.0.0.1")
                .userAgent("Test-Agent")
                .success(true)
                .timestamp(Instant.now())
                .build();
    }

    // ==================== LOG TESTS ====================

    @Test
    void log_success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        auditLogService.log(
                "user123",
                "testuser",
                AuditLog.AuditAction.LOGIN_SUCCESS,
                "Successful login",
                "127.0.0.1",
                "Test-Agent",
                true,
                null
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("user123", savedLog.getUserId());
        assertEquals("testuser", savedLog.getUsername());
        assertEquals(AuditLog.AuditAction.LOGIN_SUCCESS, savedLog.getAction());
        assertEquals("Successful login", savedLog.getDetails());
        assertEquals("127.0.0.1", savedLog.getIpAddress());
        assertEquals("Test-Agent", savedLog.getUserAgent());
        assertTrue(savedLog.isSuccess());
        assertNull(savedLog.getFailureReason());
    }

    @Test
    void log_withFailure() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        auditLogService.log(
                null,
                "testuser",
                AuditLog.AuditAction.LOGIN_FAILED,
                "Failed login attempt",
                "127.0.0.1",
                "Test-Agent",
                false,
                "Invalid password"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getUserId());
        assertFalse(savedLog.isSuccess());
        assertEquals("Invalid password", savedLog.getFailureReason());
    }

    // ==================== LOG SUCCESSFUL LOGIN TESTS ====================

    @Test
    void logSuccessfulLogin() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        auditLogService.logSuccessfulLogin("user123", "testuser", "127.0.0.1", "Test-Agent");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("user123", savedLog.getUserId());
        assertEquals(AuditLog.AuditAction.LOGIN_SUCCESS, savedLog.getAction());
        assertTrue(savedLog.isSuccess());
    }

    // ==================== LOG FAILED LOGIN TESTS ====================

    @Test
    void logFailedLogin() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        auditLogService.logFailedLogin("testuser", "Invalid password", "127.0.0.1", "Test-Agent");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getUserId());
        assertEquals("testuser", savedLog.getUsername());
        assertEquals(AuditLog.AuditAction.LOGIN_FAILED, savedLog.getAction());
        assertFalse(savedLog.isSuccess());
        assertEquals("Invalid password", savedLog.getFailureReason());
    }

    // ==================== LOG TOKEN REFRESH TESTS ====================

    @Test
    void logTokenRefresh() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        auditLogService.logTokenRefresh("user123", "testuser", "127.0.0.1", "Test-Agent");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("user123", savedLog.getUserId());
        assertEquals(AuditLog.AuditAction.TOKEN_REFRESHED, savedLog.getAction());
        assertTrue(savedLog.isSuccess());
    }

    // ==================== GET USER AUDIT LOGS TESTS ====================

    @Test
    void getUserAuditLogs_success() {
        Page<AuditLog> logPage = new PageImpl<>(List.of(testAuditLog));
        Pageable pageable = PageRequest.of(0, 10);

        when(auditLogRepository.findByUserId("user123", pageable)).thenReturn(logPage);

        Page<AuditLog> result = auditLogService.getUserAuditLogs("user123", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user123", result.getContent().get(0).getUserId());
    }

    @Test
    void getUserAuditLogs_emptyResult() {
        Page<AuditLog> emptyPage = new PageImpl<>(List.of());
        Pageable pageable = PageRequest.of(0, 10);

        when(auditLogRepository.findByUserId("unknown", pageable)).thenReturn(emptyPage);

        Page<AuditLog> result = auditLogService.getUserAuditLogs("unknown", pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== CLEANUP OLD LOGS TESTS ====================

    @Test
    void cleanupOldLogs() {
        auditLogService.cleanupOldLogs();

        verify(auditLogRepository).deleteByTimestampBefore(any(Instant.class));
    }
}

