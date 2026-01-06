package com.app.auth.service.impl;

import com.app.auth.model.AuditLog;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User user;

    @BeforeEach
    void setUp() {
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_CUSTOMER);

        user = User.builder()
                .id("user-1")
                .username("testuser")
                .roles(roles)
                .accountNonLocked(true)
                .build();
    }

    @Test
    void lockUser_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        adminService.lockUser("user-1", 30); // Lock for 30 minutes

        assertFalse(user.isAccountNonLocked());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now()));

        verify(userRepository).save(user);
        verify(auditLogService).log(
                eq("user-1"),
                eq("testuser"),
                eq(AuditLog.AuditAction.ACCOUNT_LOCKED),
                any(), any(), any(), eq(true), any()
        );
    }
}