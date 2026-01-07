package com.app.billing.controller;

import com.app.billing.dto.request.PayInvoiceRequest;
import com.app.billing.dto.response.ApiResponse;
import com.app.billing.dto.response.InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.model.PaymentMethod;
import com.app.billing.security.JwtUtil;
import com.app.billing.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private InvoiceController invoiceController;

    private InvoiceResponse invoiceResponse;
    private Authentication customerAuth;
    private Authentication managerAuth;
    private String authToken = "Bearer test-token";

    @BeforeEach
    void setUp() {
        invoiceResponse = InvoiceResponse.builder()
                .id("invoice123")
                .invoiceNumber("INV-2026-00001")
                .bookingId("booking123")
                .bookingNumber("BK-2026-00001")
                .customerId("customer123")
                .customerName("John Doe")
                .customerEmail("john@test.com")
                .customerPhone("1234567890")
                .serviceId("service123")
                .serviceName("AC Repair")
                .categoryName("HVAC")
                .technicianId("tech123")
                .technicianName("Jane Smith")
                .basePrice(1000.0)
                .taxPercentage(18.0)
                .taxAmount(180.0)
                .discountPercentage(10.0)
                .discountAmount(100.0)
                .totalAmount(1080.0)
                .currency("INR")
                .status(InvoiceStatus.PENDING)
                .isPaid(false)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(Instant.now())
                .build();

        customerAuth = new UsernamePasswordAuthenticationToken(
                "customer123", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        managerAuth = new UsernamePasswordAuthenticationToken(
                "manager123", "password",
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE_MANAGER"))
        );
    }

    // ==================== GET INVOICE BY ID TESTS ====================

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenFound() {
        when(invoiceService.getInvoiceById("invoice123")).thenReturn(invoiceResponse);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.getInvoiceById("invoice123", customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getId()).isEqualTo("invoice123");
        assertThat(response.getBody().getData().getInvoiceNumber()).isEqualTo("INV-2026-00001");
        verify(invoiceService).getInvoiceById("invoice123");
    }

    // ==================== GET INVOICE BY NUMBER TESTS ====================

    @Test
    void getInvoiceByNumber_ShouldReturnInvoice_WhenFound() {
        when(invoiceService.getInvoiceByNumber("INV-2026-00001")).thenReturn(invoiceResponse);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.getInvoiceByNumber("INV-2026-00001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getInvoiceNumber()).isEqualTo("INV-2026-00001");
        verify(invoiceService).getInvoiceByNumber("INV-2026-00001");
    }

    // ==================== GET INVOICE BY BOOKING ID TESTS ====================

    @Test
    void getInvoiceByBookingId_ShouldReturnInvoice_WhenFound() {
        when(invoiceService.getInvoiceByBookingId("booking123")).thenReturn(invoiceResponse);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.getInvoiceByBookingId("booking123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getBookingId()).isEqualTo("booking123");
        verify(invoiceService).getInvoiceByBookingId("booking123");
    }

    // ==================== GET INVOICES LIST TESTS ====================

    @Test
    void getInvoices_ShouldReturnOwnInvoices_WhenCustomerRequestsMe() {
        Page<InvoiceResponse> page = new PageImpl<>(
                List.of(invoiceResponse),
                PageRequest.of(0, 20),
                1
        );

        when(jwtUtil.extractUserId("test-token")).thenReturn("customer123");
        when(invoiceService.getInvoices(
                eq("customer123"), isNull(), eq("customer123"), eq(false), any(Pageable.class)
        )).thenReturn(page);

        ResponseEntity<ApiResponse<Page<InvoiceResponse>>> response =
                invoiceController.getInvoices("me", null, null, PageRequest.of(0, 20), authToken, customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getContent()).hasSize(1);
        verify(invoiceService).getInvoices(eq("customer123"), isNull(), eq("customer123"), eq(false), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldReturnFilteredByStatus_WhenStatusProvided() {
        Page<InvoiceResponse> page = new PageImpl<>(
                List.of(invoiceResponse),
                PageRequest.of(0, 20),
                1
        );

        when(jwtUtil.extractUserId("test-token")).thenReturn("customer123");
        when(invoiceService.getInvoices(
                eq("customer123"), eq(InvoiceStatus.PENDING), eq("customer123"), eq(false), any(Pageable.class)
        )).thenReturn(page);

        ResponseEntity<ApiResponse<Page<InvoiceResponse>>> response =
                invoiceController.getInvoices(null, null, InvoiceStatus.PENDING, PageRequest.of(0, 20), authToken, customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invoiceService).getInvoices(eq("customer123"), eq(InvoiceStatus.PENDING), eq("customer123"), eq(false), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldReturnAllInvoices_WhenManagerRequests() {
        Page<InvoiceResponse> page = new PageImpl<>(
                List.of(invoiceResponse),
                PageRequest.of(0, 20),
                1
        );

        when(jwtUtil.extractUserId("test-token")).thenReturn("manager123");
        when(invoiceService.getInvoices(
                isNull(), isNull(), eq("manager123"), eq(true), any(Pageable.class)
        )).thenReturn(page);

        ResponseEntity<ApiResponse<Page<InvoiceResponse>>> response =
                invoiceController.getInvoices(null, null, null, PageRequest.of(0, 20), authToken, managerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invoiceService).getInvoices(isNull(), isNull(), eq("manager123"), eq(true), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldFilterByCustomerId_WhenManagerRequestsSpecificCustomer() {
        Page<InvoiceResponse> page = new PageImpl<>(
                List.of(invoiceResponse),
                PageRequest.of(0, 20),
                1
        );

        when(jwtUtil.extractUserId("test-token")).thenReturn("manager123");
        when(invoiceService.getInvoices(
                eq("customer123"), isNull(), eq("manager123"), eq(true), any(Pageable.class)
        )).thenReturn(page);

        ResponseEntity<ApiResponse<Page<InvoiceResponse>>> response =
                invoiceController.getInvoices(null, "customer123", null, PageRequest.of(0, 20), authToken, managerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(invoiceService).getInvoices(eq("customer123"), isNull(), eq("manager123"), eq(true), any(Pageable.class));
    }

    // ==================== PAY INVOICE TESTS ====================

    @Test
    void payInvoice_ShouldReturnPaidInvoice_WhenPaymentSuccessful() {
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .notes("Paid via web portal")
                .build();

        InvoiceResponse paidInvoice = InvoiceResponse.builder()
                .id("invoice123")
                .invoiceNumber("INV-2026-00001")
                .status(InvoiceStatus.PAID)
                .isPaid(true)
                .paymentMethod(PaymentMethod.CARD)
                .paidAt(Instant.now())
                .build();

        when(invoiceService.payInvoice(eq("invoice123"), any(PayInvoiceRequest.class), eq("customer123")))
                .thenReturn(paidInvoice);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.payInvoice("invoice123", payRequest, customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.getBody().getData().isPaid()).isTrue();
        verify(invoiceService).payInvoice(eq("invoice123"), any(PayInvoiceRequest.class), eq("customer123"));
    }

    @Test
    void payInvoice_ShouldWorkWithUPIPayment() {
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.UPI)
                .notes("UPI payment")
                .build();

        InvoiceResponse paidInvoice = InvoiceResponse.builder()
                .id("invoice123")
                .status(InvoiceStatus.PAID)
                .paymentMethod(PaymentMethod.UPI)
                .build();

        when(invoiceService.payInvoice(eq("invoice123"), any(PayInvoiceRequest.class), eq("customer123")))
                .thenReturn(paidInvoice);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.payInvoice("invoice123", payRequest, customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    }

    @Test
    void payInvoice_ShouldWorkWithCashPayment() {
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        InvoiceResponse paidInvoice = InvoiceResponse.builder()
                .id("invoice123")
                .status(InvoiceStatus.PAID)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(invoiceService.payInvoice(eq("invoice123"), any(PayInvoiceRequest.class), eq("customer123")))
                .thenReturn(paidInvoice);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.payInvoice("invoice123", payRequest, customerAuth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    // ==================== CANCEL INVOICE TESTS ====================

    @Test
    void cancelInvoice_ShouldReturnCancelledInvoice() {
        InvoiceResponse cancelledInvoice = InvoiceResponse.builder()
                .id("invoice123")
                .invoiceNumber("INV-2026-00001")
                .status(InvoiceStatus.CANCELLED)
                .notes("Cancellation reason: Customer requested")
                .build();

        when(invoiceService.cancelInvoice("invoice123", "Customer requested"))
                .thenReturn(cancelledInvoice);

        ResponseEntity<ApiResponse<InvoiceResponse>> response =
                invoiceController.cancelInvoice("invoice123", "Customer requested");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        verify(invoiceService).cancelInvoice("invoice123", "Customer requested");
    }

    // ==================== REVENUE REPORT TESTS ====================

    @Test
    void getRevenueReport_ShouldReturnReport() {
        Map<String, Long> invoicesByStatus = new HashMap<>();
        invoicesByStatus.put("PAID", 50L);
        invoicesByStatus.put("PENDING", 20L);
        invoicesByStatus.put("CANCELLED", 5L);

        Map<String, Double> revenueByCategory = new HashMap<>();
        revenueByCategory.put("HVAC", 50000.0);
        revenueByCategory.put("Plumbing", 30000.0);
        revenueByCategory.put("Electrical", 20000.0);

        RevenueReportResponse reportResponse = RevenueReportResponse.builder()
                .totalRevenue(100000.0)
                .collectedRevenue(80000.0)
                .pendingRevenue(20000.0)
                .totalInvoices(75L)
                .paidInvoices(50L)
                .pendingInvoices(20L)
                .cancelledInvoices(5L)
                .invoicesByStatus(invoicesByStatus)
                .revenueByCategory(revenueByCategory)
                .periodStart("2026-01-01")
                .periodEnd("2026-01-31")
                .build();

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        when(invoiceService.getRevenueReport(startDate, endDate)).thenReturn(reportResponse);

        ResponseEntity<ApiResponse<RevenueReportResponse>> response =
                invoiceController.getRevenueReport(startDate, endDate);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getTotalRevenue()).isEqualTo(100000.0);
        assertThat(response.getBody().getData().getCollectedRevenue()).isEqualTo(80000.0);
        assertThat(response.getBody().getData().getPendingRevenue()).isEqualTo(20000.0);
        assertThat(response.getBody().getData().getTotalInvoices()).isEqualTo(75L);
        verify(invoiceService).getRevenueReport(startDate, endDate);
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void searchInvoices_ShouldReturnMatchingInvoices() {
        List<InvoiceResponse> searchResults = Arrays.asList(invoiceResponse);
        when(invoiceService.searchInvoices("John")).thenReturn(searchResults);

        ResponseEntity<ApiResponse<List<InvoiceResponse>>> response =
                invoiceController.searchInvoices("John");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).hasSize(1);
        verify(invoiceService).searchInvoices("John");
    }

    @Test
    void searchInvoices_ShouldReturnEmptyList_WhenNoMatches() {
        when(invoiceService.searchInvoices("NonExistent")).thenReturn(List.of());

        ResponseEntity<ApiResponse<List<InvoiceResponse>>> response =
                invoiceController.searchInvoices("NonExistent");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isEmpty();
    }

    @Test
    void searchInvoices_ShouldSearchByInvoiceNumber() {
        List<InvoiceResponse> searchResults = Arrays.asList(invoiceResponse);
        when(invoiceService.searchInvoices("INV-2026")).thenReturn(searchResults);

        ResponseEntity<ApiResponse<List<InvoiceResponse>>> response =
                invoiceController.searchInvoices("INV-2026");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).hasSize(1);
    }
}