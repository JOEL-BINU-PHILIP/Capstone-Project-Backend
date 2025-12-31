package com.app.auth.service;

import com.app.auth.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    void log(
            String userId,
            String username,
            AuditLog.AuditAction action,
            String details,
            String ipAddress,
            String userAgent,
            boolean success,
            String failureReason
    );

    void logSuccessfulLogin(String userId, String username, String ipAddress, String userAgent);

    void logFailedLogin(String username, String reason, String ipAddress, String userAgent);

    void logTokenRefresh(String userId, String username, String ipAddress, String userAgent);

    Page<AuditLog> getUserAuditLogs(String userId, Pageable pageable);

    void cleanupOldLogs();
}