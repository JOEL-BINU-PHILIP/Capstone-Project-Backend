package com.app.auth.controller;

import com.app.auth.dto.response.UserProfileResponseDTO;
import com. app.auth.model.AuditLog;
import com.app. auth.model.User;
import com. app.auth.model.UserRole;
import com. app.auth.payload.ApiResponse;
import com.app.auth. repository.AuditLogRepository;
import com.app.auth.repository. UserRepository;
import com.app.auth.service.AdminService;
import com.app.auth. service.AuditLogService;
import com.app.auth. service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;
import org.springframework. http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org. springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    // ==================== USER MANAGEMENT ====================

    /**
     * Get all users (paginated)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users retrieved successfully", users)
        );
    }

    /**
     * Get user by ID
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> getUserById(
            @PathVariable("userId") String userId
    ) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.ok(
                    new ApiResponse<>(false, "User not found", null)
            );
        }

        return ResponseEntity. ok(
                new ApiResponse<>(true, "User retrieved successfully", user)
        );
    }

    /**
     * Get users by role
     */
    @GetMapping("/users/by-role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<User>>> getUsersByRole(
            @RequestParam("role") UserRole role,
            Pageable pageable
    ) {
        Page<User> users = userRepository.findByRole(role, pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Users retrieved successfully", users)
        );
    }
    @DeleteMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable("userId") String userId,
            @RequestParam("role") UserRole role
    ) {
        adminService.removeRole(userId, role);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Role removed successfully", null)
        );
    }

    // ==================== ACCOUNT LOCK/UNLOCK ====================

    @PostMapping("/users/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> lockUser(
            @PathVariable("userId") String userId,
            @RequestParam(name = "durationMinutes", required = false, defaultValue = "30") long durationMinutes
    ) {
        adminService.lockUser(userId, durationMinutes);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User locked successfully", null)
        );
    }

    @PostMapping("/users/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable("userId") String userId
    ) {
        adminService.unlockUser(userId);

        return ResponseEntity. ok(
                new ApiResponse<>(true, "User unlocked successfully", null)
        );
    }

    /**
     * Get all locked accounts
     */
    @GetMapping("/users/locked")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<User>>> getLockedAccounts() {
        List<User> lockedUsers = userRepository.findLockedAccounts();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Locked accounts retrieved", lockedUsers)
        );
    }

    // ==================== AUDIT LOGS ====================

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(name = "userId", required = false) String userId,
            Pageable pageable
    ) {
        Page<AuditLog> logs;

        if (userId != null && !userId.isEmpty()) {
            logs = auditLogService.getUserAuditLogs(userId, pageable);
        } else {
            logs = auditLogRepository.findAll(pageable);
        }

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Audit logs retrieved", logs)
        );
    }

    /**
     * Get audit logs by action type
     */
    @GetMapping("/audit-logs/by-action")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogsByAction(
            @RequestParam("action") AuditLog.AuditAction action,
            Pageable pageable
    ) {
        Page<AuditLog> logs = auditLogRepository.findByAction(action, pageable);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Audit logs retrieved", logs)
        );
    }

    /**
     * Get audit logs by IP address
     */
    @GetMapping("/audit-logs/by-ip")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAuditLogsByIp(
            @RequestParam("ipAddress") String ipAddress
    ) {
        List<AuditLog> logs = auditLogRepository.findByIpAddress(ipAddress);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Audit logs retrieved", logs)
        );
    }
}