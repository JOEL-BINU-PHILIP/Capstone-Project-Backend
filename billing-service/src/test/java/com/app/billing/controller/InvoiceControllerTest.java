package com.app.billing.controller;

import com.app.billing.dto.request.PayInvoiceRequest;
import com.app.billing.dto.response.InvoiceResponse;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.model.PaymentMethod;
import com.app.billing.security.JwtAuthenticationFilter;
import com.app.billing.security.JwtUtil;
import com.app.billing.service.InvoiceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit testing
public class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private InvoiceResponse invoiceResponse;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        invoiceResponse = InvoiceResponse.builder()
                .id("inv-123")
                .invoiceNumber("INV-2026-00001")
                .bookingId("book-123")
                .totalAmount(1000.0)
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        auth = new UsernamePasswordAuthenticationToken("user", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    void getInvoiceById_Success() throws Exception {
        when(invoiceService.getInvoiceById("inv-123")).thenReturn(invoiceResponse);

        mockMvc.perform(get("/api/billing/invoices/{invoiceId}", "inv-123")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("INV-2026-00001"));
    }

    @Test
    void getInvoices_Success() throws Exception {
        Page<InvoiceResponse> page = new PageImpl<>(List.of(invoiceResponse), PageRequest.of(0, 10), 1);

        // Mock JWT util for extracting userId
        when(jwtUtil.extractUserId(anyString())).thenReturn("user-123");
        when(invoiceService.getInvoices(any(), any(), anyString(), anyBoolean(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/billing/invoices")
                        .header("Authorization", "Bearer token")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("inv-123"));
    }

    @Test
    void payInvoice_Success() throws Exception {
        PayInvoiceRequest request = new PayInvoiceRequest(PaymentMethod.CARD, "Paid via web");

        InvoiceResponse paidResponse = InvoiceResponse.builder()
                .id("inv-123")
                .status(InvoiceStatus.PAID)
                .build();

        when(invoiceService.payInvoice(eq("inv-123"), any(PayInvoiceRequest.class), anyString()))
                .thenReturn(paidResponse);

        mockMvc.perform(post("/api/billing/invoices/{invoiceId}/pay", "inv-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void cancelInvoice_Success() throws Exception {
        InvoiceResponse cancelledResponse = InvoiceResponse.builder()
                .id("inv-123")
                .status(InvoiceStatus.CANCELLED)
                .build();

        when(invoiceService.cancelInvoice(eq("inv-123"), anyString()))
                .thenReturn(cancelledResponse);

        mockMvc.perform(post("/api/billing/invoices/{invoiceId}/cancel", "inv-123")
                        .param("reason", "Mistake"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}