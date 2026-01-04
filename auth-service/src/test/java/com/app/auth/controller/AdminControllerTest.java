package com.app.auth.controller;

import com.app.auth.model.User;
import com.app.auth.repository.AuditLogRepository;
import com.app.auth.repository.UserRepository;
import com.app.auth.security.JwtAuthenticationFilter;
import com.app.auth.security.JwtUtils;
import com.app.auth.service.AdminService;
import com.app.auth.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AdminService adminService;
    @MockBean private AuditLogService auditLogService;
    @MockBean private UserRepository userRepository;
    @MockBean private AuditLogRepository auditLogRepository;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllUsers_Success() throws Exception {
        User user1 = User.builder().id("1").username("user1").email("u1@test.com").build();
        User user2 = User.builder().id("2").username("user2").email("u2@test.com").build();

        // FIX: Use PageRequest.of(...) to prevent UnsupportedOperationException during serialization
        Page<User> userPage = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 10), 2);

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].username").value("user1"));
    }

    @Test
    void lockUser_Success() throws Exception {
        String userId = "user-123";
        long duration = 60; // minutes

        mockMvc.perform(post("/api/admin/users/{userId}/lock", userId)
                        .param("durationMinutes", String.valueOf(duration)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminService).lockUser(eq(userId), eq(duration));
    }
}