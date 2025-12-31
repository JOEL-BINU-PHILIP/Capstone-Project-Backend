package com.app.auth.controller;

import com.app.auth.model.AuditLog;
import com.app.auth.model.UserRole;
import com.app.auth.payload.ApiResponse;
import com.app.auth.service.AdminService;
import com.app.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignRole(
            @PathVariable String userId,
            @RequestParam UserRole role
    ) {
        adminService.assignRole(userId, role);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Role assigned successfully", null)
        );
    }

    @DeleteMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable String userId,
            @RequestParam UserRole role
    ) {
        adminService.removeRole(userId, role);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Role removed successfully", null)
        );
    }

    @PostMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable String userId,
            @RequestParam(required = false, defaultValue = "30") long durationMinutes
    ) {
        adminService.lockUser(userId, durationMinutes);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User locked successfully", null)
        );
    }

    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable String userId
    ) {
        adminService.unlockUser(userId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User unlocked successfully", null)
        );
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) String userId,
            Pageable pageable
    ) {
        Page<AuditLog> logs;

        if (userId != null) {
            logs = auditLogService.getUserAuditLogs(userId, pageable);
        } else {
            // Would need to implement this method
            logs = Page.empty();
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Audit logs retrieved", logs)
        );
    }
}