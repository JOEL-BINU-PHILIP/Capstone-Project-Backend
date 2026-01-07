package com.app.auth.service.impl;

import com.app.auth.exception.ResourceNotFoundException;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTestExtended {

    @InjectMocks
    private AdminServiceImpl adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_CUSTOMER);
        roles.add(UserRole.ROLE_TECHNICIAN);

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .roles(roles)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();
    }

    // ==================== REMOVE ROLE TESTS ====================

    @Test
    void removeRole_success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        adminService.removeRole("user123", UserRole.ROLE_TECHNICIAN);

        assertFalse(testUser.getRoles().contains(UserRole.ROLE_TECHNICIAN));
        assertTrue(testUser.getRoles().contains(UserRole.ROLE_CUSTOMER));
        verify(userRepository).save(testUser);
        verify(auditLogService).log(eq("user123"), eq("testuser"), eq(AuditLog.AuditAction.ROLE_CHANGED),
                contains("Role removed"), isNull(), isNull(), eq(true), isNull());
    }

    @Test
    void removeRole_userNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.removeRole("unknown", UserRole.ROLE_CUSTOMER)
        );
    }

    @Test
    void removeRole_userDoesNotHaveRole() {
        testUser.setRoles(Set.of(UserRole.ROLE_CUSTOMER));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalStateException.class, () ->
                adminService.removeRole("user123", UserRole.ROLE_ADMIN)
        );
    }

    @Test
    void removeRole_lastRole() {
        testUser.setRoles(new HashSet<>(Set.of(UserRole.ROLE_CUSTOMER)));
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalStateException.class, () ->
                adminService.removeRole("user123", UserRole.ROLE_CUSTOMER)
        );
    }

    // ==================== LOCK USER TESTS ====================

    @Test
    void lockUser_success() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        adminService.lockUser("user123", 60);

        assertFalse(testUser.isAccountNonLocked());
        assertNotNull(testUser.getLockedUntil());
        assertTrue(testUser.getLockedUntil().isAfter(Instant.now()));
        assertTrue(testUser.getLockedUntil().isBefore(Instant.now().plusSeconds(3700)));
        verify(userRepository).save(testUser);
        verify(auditLogService).log(eq("user123"), eq("testuser"), eq(AuditLog.AuditAction.ACCOUNT_LOCKED),
                contains("60 minutes"), isNull(), isNull(), eq(true), isNull());
    }

    @Test
    void lockUser_userNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.lockUser("unknown", 30)
        );
    }

    @Test
    void lockUser_differentDuration() {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        adminService.lockUser("user123", 120);

        assertFalse(testUser.isAccountNonLocked());
        assertNotNull(testUser.getLockedUntil());
        verify(auditLogService).log(anyString(), anyString(), eq(AuditLog.AuditAction.ACCOUNT_LOCKED),
                contains("120 minutes"), any(), any(), eq(true), any());
    }

    // ==================== UNLOCK USER TESTS ====================

    @Test
    void unlockUser_success() {
        testUser.setAccountNonLocked(false);
        testUser.setLockedUntil(Instant.now().plusSeconds(3600));
        testUser.setFailedLoginAttempts(5);

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        adminService.unlockUser("user123");

        assertTrue(testUser.isAccountNonLocked());
        assertNull(testUser.getLockedUntil());
        assertEquals(0, testUser.getFailedLoginAttempts());
        verify(userRepository).save(testUser);
        verify(auditLogService).log(eq("user123"), eq("testuser"), eq(AuditLog.AuditAction.ACCOUNT_UNLOCKED),
                contains("unlocked by admin"), isNull(), isNull(), eq(true), isNull());
    }

    @Test
    void unlockUser_userNotFound() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                adminService.unlockUser("unknown")
        );
    }

    @Test
    void unlockUser_alreadyUnlocked() {
        testUser.setAccountNonLocked(true);
        testUser.setLockedUntil(null);

        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // Should still work without throwing exception
        adminService.unlockUser("user123");

        assertTrue(testUser.isAccountNonLocked());
        verify(userRepository).save(testUser);
    }
}

