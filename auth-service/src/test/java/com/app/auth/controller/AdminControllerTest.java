package com.app.auth.controller;

import com.app.auth.config.TestSecurityConfig;
import com.app.auth.model.AuditLog;
import com.app.auth.model.User;
import com.app.auth.model.UserRole;
import com.app.auth.payload.ApiResponse;
import com.app.auth.repository.AuditLogRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AdminService;
import com.app.auth.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private AuditLogService auditLogService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@test.com")
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(UserRole.ROLE_CUSTOMER))
                .enabled(true)
                .emailVerified(true)
                .accountNonLocked(true)
                .build();

        testAuditLog = AuditLog.builder()
                .id("log123")
                .userId("user123")
                .username("testuser")
                .action(AuditLog.AuditAction.LOGIN_SUCCESS)
                .details("Successful login")
                .ipAddress("127.0.0.1")
                .success(true)
                .timestamp(Instant.now())
                .build();
    }

    // ==================== USER MANAGEMENT TESTS ====================

    @Test
    void getAllUsers_success() throws Exception {
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"));
    }

    @Test
    void getUserById_success() throws Exception {
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        mockMvc.perform(get("/api/admin/users/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"));
    }

    @Test
    void getUserById_notFound() throws Exception {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/admin/users/nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void getUsersByRole_success() throws Exception {
        Page<User> userPage = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
        when(userRepository.findByRole(eq(UserRole.ROLE_CUSTOMER), any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users/by-role")
                        .param("role", "ROLE_CUSTOMER")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void removeRole_success() throws Exception {
        doNothing().when(adminService).removeRole(anyString(), any(UserRole.class));

        mockMvc.perform(delete("/api/admin/users/user123/roles")
                        .param("role", "ROLE_TECHNICIAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role removed successfully"));
    }

    // ==================== ACCOUNT LOCK/UNLOCK TESTS ====================

    @Test
    void lockUser_success() throws Exception {
        doNothing().when(adminService).lockUser(anyString(), anyLong());

        mockMvc.perform(post("/api/admin/users/user123/lock")
                        .param("durationMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User locked successfully"));
    }

    @Test
    void lockUser_defaultDuration() throws Exception {
        doNothing().when(adminService).lockUser(anyString(), anyLong());

        mockMvc.perform(post("/api/admin/users/user123/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void unlockUser_success() throws Exception {
        doNothing().when(adminService).unlockUser(anyString());

        mockMvc.perform(post("/api/admin/users/user123/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User unlocked successfully"));
    }

    @Test
    void getLockedAccounts_success() throws Exception {
        User lockedUser = User.builder()
                .id("locked123")
                .username("lockeduser")
                .accountNonLocked(false)
                .lockedUntil(Instant.now().plusSeconds(3600))
                .build();

        when(userRepository.findLockedAccounts()).thenReturn(List.of(lockedUser));

        mockMvc.perform(get("/api/admin/users/locked"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Locked accounts retrieved"));
    }

    // ==================== AUDIT LOGS TESTS ====================

    @Test
    void getAuditLogs_success() throws Exception {
        Page<AuditLog> logPage = new PageImpl<>(List.of(testAuditLog), PageRequest.of(0, 10), 1);
        when(auditLogRepository.findAll(any(Pageable.class))).thenReturn(logPage);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Audit logs retrieved"));
    }

    @Test
    void getAuditLogs_withUserId() throws Exception {
        Page<AuditLog> logPage = new PageImpl<>(List.of(testAuditLog), PageRequest.of(0, 10), 1);
        when(auditLogService.getUserAuditLogs(eq("user123"), any(Pageable.class))).thenReturn(logPage);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("userId", "user123")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAuditLogsByAction_success() throws Exception {
        Page<AuditLog> logPage = new PageImpl<>(List.of(testAuditLog), PageRequest.of(0, 10), 1);
        when(auditLogRepository.findByAction(eq(AuditLog.AuditAction.LOGIN_SUCCESS), any(Pageable.class)))
                .thenReturn(logPage);

        mockMvc.perform(get("/api/admin/audit-logs/by-action")
                        .param("action", "LOGIN_SUCCESS")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAuditLogsByIp_success() throws Exception {
        when(auditLogRepository.findByIpAddress("127.0.0.1")).thenReturn(List.of(testAuditLog));

        mockMvc.perform(get("/api/admin/audit-logs/by-ip")
                        .param("ipAddress", "127.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Audit logs retrieved"));
    }
}

