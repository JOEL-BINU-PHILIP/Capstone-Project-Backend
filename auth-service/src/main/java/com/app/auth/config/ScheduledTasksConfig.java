package com.app.auth.config;

import com.app.auth.service.AuditLogService;
import com.app.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@RequiredArgsConstructor
public class ScheduledTasksConfig {

    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupOldAuditLogs() {
        auditLogService.cleanupOldLogs(); // This method must exist on the interface!
    }

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void cleanupExpiredTokens() {
        refreshTokenService.cleanupExpiredTokens(); // This method must exist on the interface!
    }
}