package com.app.booking.controller;

import com.app.booking.dto.request.AssignTechnicianRequest;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.model.BookingStatus;
import com.app.booking.security.JwtUtil;
import com.app.booking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Part 4: Manager Action Tests
 */
@WebMvcTest(BookingController.class)
class BookingControllerTest_ManagerActions {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtUtil jwtUtil;

    private BookingResponse bookingResponse;
    private String authToken = "Bearer test-token";

    @BeforeEach
    void setUp() {
        bookingResponse = BookingResponse.builder()
                .id("booking123")
                .bookingNumber("BK-2026-00001")
                .status(BookingStatus.PENDING)
                .technicianId("tech123")
                .technicianName("Jane Smith")
                .createdAt(Instant.now())
                .build();

        when(jwtUtil.extractUserId(anyString())).thenReturn("manager123");
    }

    // ==================== ASSIGN TECHNICIAN TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void assignTechnician_ShouldReturnAssignedBooking_WhenValid() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        bookingResponse.setStatus(BookingStatus.ASSIGNED);
        when(bookingService.assignTechnician(
                eq("booking123"), any(AssignTechnicianRequest.class), eq("manager123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/assign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Technician assigned successfully"))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.technicianId").value("tech123"));

        verify(bookingService, times(1)).assignTechnician(
                eq("booking123"), any(AssignTechnicianRequest.class), eq("manager123")
        );
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignTechnician_ShouldWork_WhenAdmin() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        when(bookingService.assignTechnician(
                eq("booking123"), any(AssignTechnicianRequest.class), eq("manager123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/assign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void assignTechnician_ShouldReturnForbidden_WhenNotManager() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech123")
                .build();

        mockMvc.perform(post("/api/bookings/booking123/assign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).assignTechnician(anyString(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void assignTechnician_ShouldReturnBadRequest_WhenMissingTechnicianId() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .build(); // Missing technicianId

        mockMvc.perform(post("/api/bookings/booking123/assign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== REASSIGN TECHNICIAN TESTS ====================

    @Test
    @WithMockUser(username = "manager", roles = {"SERVICE_MANAGER"})
    void reassignTechnician_ShouldReturnReassignedBooking_WhenValid() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech456")
                .build();

        bookingResponse.setTechnicianId("tech456");
        bookingResponse.setStatus(BookingStatus.ASSIGNED);

        when(bookingService.reassignTechnician(
                eq("booking123"), any(AssignTechnicianRequest.class), eq("manager123")
        )).thenReturn(bookingResponse);

        mockMvc.perform(post("/api/bookings/booking123/reassign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$. message").value("Technician reassigned successfully"))
                .andExpect(jsonPath("$.data. technicianId").value("tech456"));

        verify(bookingService, times(1)).reassignTechnician(
                eq("booking123"), any(AssignTechnicianRequest.class), eq("manager123")
        );
    }

    @Test
    @WithMockUser(username = "technician", roles = {"TECHNICIAN"})
    void reassignTechnician_ShouldReturnForbidden_WhenNotManager() throws Exception {
        AssignTechnicianRequest request = AssignTechnicianRequest.builder()
                .technicianId("tech456")
                .build();

        mockMvc.perform(post("/api/bookings/booking123/reassign")
                        .with(csrf())
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
