package com.app.auth.service.impl;

import com.app.auth.exception.ResourceNotFoundException;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.repository.UserRepository;
import com.app.auth.service.AuditLogService;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService; // 🔴 FIX

    // =========================
    // LOCK USER
    // =========================
    @Test
    void lockUser_success() {

        User user = User.builder()
                .id("u1")
                .accountNonLocked(true)
                .build();

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(user));

        adminService.lockUser("u1", 30);

        assertFalse(user.isAccountNonLocked());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(Instant.now()));

        verify(userRepository).save(user);
        verify(auditLogService).log(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    // =========================
    // UNLOCK USER
    // =========================
    @Test
    void unlockUser_success() {

        User user = User.builder()
                .id("u1")
                .accountNonLocked(false)
                .lockedUntil(Instant.now().plusSeconds(600))
                .build();

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(user));

        adminService.unlockUser("u1");

        assertTrue(user.isAccountNonLocked());
        assertNull(user.getLockedUntil());

        verify(userRepository).save(user);
        verify(auditLogService).log(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
    }

    // =========================
    // REMOVE ROLE (FIXED)
    // =========================
    @Test
    void removeRole_success() {

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_CUSTOMER);
        roles.add(UserRole.ROLE_TECHNICIAN);

        User user = User.builder()
                .id("u1")
                .roles(roles)
                .build();

        when(userRepository.findById("u1"))
                .thenReturn(Optional.of(user));

        adminService.removeRole("u1", UserRole.ROLE_TECHNICIAN);

        assertFalse(user.getRoles().contains(UserRole.ROLE_TECHNICIAN));
        assertTrue(user.getRoles().contains(UserRole.ROLE_CUSTOMER));

        verify(userRepository).save(user);
    }

    // =========================
    // USER NOT FOUND
    // =========================
    @Test
    void lockUser_userNotFound() {

        when(userRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> adminService.lockUser("missing", 10)
        );
    }
}
