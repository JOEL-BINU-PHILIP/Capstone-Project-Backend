package com.app.auth.service.impl;

import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.AuditLog;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.AdminService;
import com.app.auth.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public void removeRole(String userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getRoles().contains(role)) {
            throw new IllegalStateException("User doesn't have this role");
        }

        if (user.getRoles().size() == 1) {
            throw new IllegalStateException("Cannot remove last role from user");
        }

        user.getRoles().remove(role);
        userRepository.save(user);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.ROLE_CHANGED,
                "Role removed: " + role.name(),
                null,
                null,
                true,
                null
        );

        log.info("Role {} removed from user: {}", role, user.getUsername());
    }

    @Override
    @Transactional
    public void lockUser(String userId, long durationMinutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.lockAccount(durationMinutes);
        userRepository.save(user);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.ACCOUNT_LOCKED,
                "Account locked by admin for " + durationMinutes + " minutes",
                null,
                null,
                true,
                null
        );

        log.info("User {} locked for {} minutes", user.getUsername(), durationMinutes);
    }

    @Override
    @Transactional
    public void unlockUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setAccountNonLocked(true);
        user.setLockedUntil(null);
        user.resetFailedAttempts();
        userRepository.save(user);

        auditLogService.log(
                user.getId(),
                user.getUsername(),
                AuditLog.AuditAction.ACCOUNT_UNLOCKED,
                "Account unlocked by admin",
                null,
                null,
                true,
                null
        );

        log.info("User {} unlocked", user.getUsername());
    }
}