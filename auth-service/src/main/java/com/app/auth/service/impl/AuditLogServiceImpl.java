package com.app.auth.service.impl;

import com.app.auth.model.AuditLog;
import com.app.auth.repository.AuditLogRepository;
import com.app.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    public void log(
            String userId,
            String username,
            AuditLog.AuditAction action,
            String details,
            String ipAddress,
            String userAgent,
            boolean success,
            String failureReason
    ) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .username(username)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .failureReason(failureReason)
                .build();

        auditLogRepository.save(log);
    }

    @Override
    public void logSuccessfulLogin(String userId, String username, String ipAddress, String userAgent) {
        log(userId, username, AuditLog.AuditAction.LOGIN_SUCCESS,
                "Successful login", ipAddress, userAgent, true, null);
    }

    @Override
    public void logFailedLogin(String username, String reason, String ipAddress, String userAgent) {
        log(null, username, AuditLog.AuditAction.LOGIN_FAILED,
                "Failed login attempt", ipAddress, userAgent, false, reason);
    }

    @Override
    public void logTokenRefresh(String userId, String username, String ipAddress, String userAgent) {
        log(userId, username, AuditLog.AuditAction.TOKEN_REFRESHED,
                "Access token refreshed", ipAddress, userAgent, true, null);
    }

    @Override
    public Page<AuditLog> getUserAuditLogs(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public void cleanupOldLogs() {
        // Keep logs for 90 days
        Instant cutoffDate = Instant.now().minus(90, ChronoUnit.DAYS);
        auditLogRepository.deleteByTimestampBefore(cutoffDate);
        log.info("Cleaned up audit logs older than 90 days");
    }
}

