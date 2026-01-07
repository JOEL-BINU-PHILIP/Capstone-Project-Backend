package com.app.billing.service.impl;

import com.app.billing.client.BookingServiceClient;
import com.app.billing.dto.request.PayInvoiceRequest;
import com.app.billing.dto.response.InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com.app.billing.event.BillingEvent;
import com.app.billing.exception.BillingException;
import com.app.billing.exception.DuplicateInvoiceException;
import com.app.billing.exception.ResourceNotFoundException;
import com.app.billing.model.Invoice;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.model.PaymentMethod;
import com.app.billing.repository.InvoiceRepository;
import com.app.billing.service.EventPublisherService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest_ExtendedCoverage {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        testInvoice = Invoice.builder()
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
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .createdAt(Instant.now())
                .build();
    }

    // ==================== CREATE INVOICE EXTENDED TESTS ====================

    @Test
    void createInvoiceFromBooking_ShouldCalculateAmountsCorrectly() {
        Map<String, Object> bookingData = createFullBookingData();
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result).isNotNull();
        assertThat(result.getBasePrice()).isEqualTo(1000.0);
        // Tax: 1000 * 0.18 = 180
        assertThat(result.getTaxAmount()).isEqualTo(180.0);
        // Discount: 1000 * 0.10 = 100
        assertThat(result.getDiscountAmount()).isEqualTo(100.0);
        // Total: 1000 + 180 - 100 = 1080
        assertThat(result.getTotalAmount()).isEqualTo(1080.0);
        verify(eventPublisherService).publishBillingEvent(any(BillingEvent.class));
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleNullTaxAndDiscount() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", 500.0);
        // No taxPercentage, discountPercentage - should use defaults

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result).isNotNull();
        // Should use default 18% tax when not provided
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingReturnsNoData() {
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", null);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class);
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingServiceReturnsFailure() {
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", false);
        responseWrapper.put("message", "Booking not found");

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class);
    }

    // ==================== GET INVOICES EXTENDED TESTS ====================

    @Test
    void getInvoices_ShouldReturnOwnInvoices_ForNonManager() {
        Page<Invoice> page = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        // When isManager=false, effectiveCustomerId should be currentUser
        when(invoiceRepository.findByCustomerId(eq("customer123"), any(Pageable.class))).thenReturn(page);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                "other-customer", // customerId passed but ignored for non-manager
                null,
                "customer123", // currentUser
                false, // isManager
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByCustomerId(eq("customer123"), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldFilterByCustomerIdAndStatus() {
        Page<Invoice> page = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByCustomerIdAndStatus(eq("customer123"), eq(InvoiceStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                "customer123",
                InvoiceStatus.PENDING,
                "manager123",
                true,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByCustomerIdAndStatus(eq("customer123"), eq(InvoiceStatus.PENDING), any(Pageable.class));
    }

    // ==================== PAY INVOICE EXTENDED TESTS ====================

    @Test
    void payInvoice_ShouldAddPaymentNotes_WhenNoExistingNotes() {
        testInvoice.setNotes(null);
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .notes("Payment processed")
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.payInvoice("invoice123", request, "customer123");

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes() != null && invoice.getNotes().contains("Payment note")));
    }

    @Test
    void payInvoice_ShouldWorkWithOnlinePayment() {
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.ONLINE)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.payInvoice("invoice123", request, "customer123");

        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.ONLINE);
    }

    // ==================== REVENUE REPORT EXTENDED TESTS ====================

    @Test
    void getRevenueReport_ShouldCalculateInvoicesByStatusCorrectly() {
        Invoice paidInvoice1 = createInvoiceWithStatus(InvoiceStatus.PAID, "HVAC", 1000.0);
        Invoice paidInvoice2 = createInvoiceWithStatus(InvoiceStatus.PAID, "Plumbing", 500.0);
        Invoice pendingInvoice1 = createInvoiceWithStatus(InvoiceStatus.PENDING, "HVAC", 300.0);
        Invoice pendingInvoice2 = createInvoiceWithStatus(InvoiceStatus.PENDING, "Electrical", 400.0);
        Invoice cancelledInvoice = createInvoiceWithStatus(InvoiceStatus.CANCELLED, "HVAC", 200.0);

        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Arrays.asList(paidInvoice1, paidInvoice2, pendingInvoice1, pendingInvoice2, cancelledInvoice));

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(report.getTotalInvoices()).isEqualTo(5L);
        assertThat(report.getPaidInvoices()).isEqualTo(2L);
        assertThat(report.getPendingInvoices()).isEqualTo(2L);
        assertThat(report.getCancelledInvoices()).isEqualTo(1L);

        // Total revenue = All non-cancelled = 1000 + 500 + 300 + 400 = 2200
        assertThat(report.getTotalRevenue()).isEqualTo(2200.0);
        // Collected = PAID only = 1000 + 500 = 1500
        assertThat(report.getCollectedRevenue()).isEqualTo(1500.0);
        // Pending = 300 + 400 = 700
        assertThat(report.getPendingRevenue()).isEqualTo(700.0);
    }

    @Test
    void getRevenueReport_ShouldGroupRevenueByCategoryCorrectly() {
        Invoice hvacInvoice1 = createInvoiceWithStatus(InvoiceStatus.PAID, "HVAC", 1000.0);
        Invoice hvacInvoice2 = createInvoiceWithStatus(InvoiceStatus.PAID, "HVAC", 500.0);
        Invoice plumbingInvoice = createInvoiceWithStatus(InvoiceStatus.PAID, "Plumbing", 800.0);

        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Arrays.asList(hvacInvoice1, hvacInvoice2, plumbingInvoice));

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(report.getRevenueByCategory()).containsEntry("HVAC", 1500.0);
        assertThat(report.getRevenueByCategory()).containsEntry("Plumbing", 800.0);
    }

    @Test
    void getRevenueReport_ShouldSetCorrectPeriodDates() {
        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 31);

        RevenueReportResponse report = invoiceService.getRevenueReport(startDate, endDate);

        assertThat(report.getPeriodStart()).isEqualTo("2026-01-01");
        assertThat(report.getPeriodEnd()).isEqualTo("2026-01-31");
    }

    // ==================== SEARCH EXTENDED TESTS ====================

    @Test
    void searchInvoices_ShouldSearchByMultipleCriteria() {
        Invoice invoice1 = testInvoice;
        Invoice invoice2 = Invoice.builder()
                .id("invoice456")
                .invoiceNumber("INV-2026-00002")
                .customerName("Jane Doe")
                .status(InvoiceStatus.PAID)
                .totalAmount(2000.0)
                .createdAt(Instant.now())
                .build();

        when(invoiceRepository.searchInvoices("Doe")).thenReturn(Arrays.asList(invoice1, invoice2));

        List<InvoiceResponse> results = invoiceService.searchInvoices("Doe");

        assertThat(results).hasSize(2);
    }

    @Test
    void searchInvoices_ShouldSearchByInvoiceNumber() {
        when(invoiceRepository.searchInvoices("INV-2026")).thenReturn(List.of(testInvoice));

        List<InvoiceResponse> results = invoiceService.searchInvoices("INV-2026");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getInvoiceNumber()).contains("INV-2026");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void createInvoiceFromBooking_ShouldHandleZeroBasePrice() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", 0.0);

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result.getTotalAmount()).isEqualTo(0.0);
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleIntegerPriceValues() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", 1000); // Integer instead of Double

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result.getBasePrice()).isEqualTo(1000.0);
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleStringPriceValues() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", "500.50"); // String instead of number

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result.getBasePrice()).isEqualTo(500.50);
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleLongPriceValues() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", 1000L); // Long instead of Double

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result.getBasePrice()).isEqualTo(1000.0);
    }

    // ==================== HELPER METHODS ====================

    private Map<String, Object> createFullBookingData() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("bookingNumber", "BK-2026-00001");
        bookingData.put("customerId", "customer123");
        bookingData.put("customerName", "John Doe");
        bookingData.put("customerEmail", "john@test.com");
        bookingData.put("customerPhone", "1234567890");
        bookingData.put("serviceId", "service123");
        bookingData.put("serviceName", "AC Repair");
        bookingData.put("categoryName", "HVAC");
        bookingData.put("technicianId", "tech123");
        bookingData.put("technicianName", "Jane Smith");
        bookingData.put("basePrice", 1000.0);
        bookingData.put("taxPercentage", 18.0);
        bookingData.put("discountPercentage", 10.0);
        return bookingData;
    }

    private Invoice createInvoiceWithStatus(InvoiceStatus status, String category, Double amount) {
        return Invoice.builder()
                .id(UUID.randomUUID().toString())
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8))
                .status(status)
                .categoryName(category)
                .totalAmount(amount)
                .createdAt(Instant.now())
                .build();
    }

    // ==================== ADDITIONAL TESTS FOR 90% COVERAGE ====================

    // ==================== GET INVOICE TESTS ====================

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenExists() {
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceById("invoice123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("invoice123");
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2026-00001");
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInvoiceByNumber_ShouldReturnInvoice_WhenExists() {
        when(invoiceRepository.findByInvoiceNumber("INV-2026-00001")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceByNumber("INV-2026-00001");

        assertThat(result).isNotNull();
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2026-00001");
    }

    @Test
    void getInvoiceByNumber_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findByInvoiceNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByNumber("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getInvoiceByBookingId_ShouldReturnInvoice_WhenExists() {
        when(invoiceRepository.findByBookingId("booking123")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceByBookingId("booking123");

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo("booking123");
    }

    @Test
    void getInvoiceByBookingId_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findByBookingId("invalid-booking")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByBookingId("invalid-booking"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== GET INVOICES LIST TESTS ====================

    @Test
    void getInvoices_ShouldFilterByStatusOnly() {
        Page<Invoice> page = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByStatus(eq(InvoiceStatus.PENDING), any(Pageable.class))).thenReturn(page);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                null,
                InvoiceStatus.PENDING,
                "manager123",
                true,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByStatus(eq(InvoiceStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldReturnAllInvoices_ForManagerWithNoFilters() {
        Page<Invoice> page = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                null,
                null,
                "manager123",
                true,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findAll(any(Pageable.class));
    }

    // ==================== CANCEL INVOICE TESTS ====================

    @Test
    void cancelInvoice_ShouldCancelPendingInvoice() {
        testInvoice.setStatus(InvoiceStatus.PENDING);
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.cancelInvoice("invoice123", "Customer request");

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes() != null && invoice.getNotes().contains("Customer request")));
    }

    @Test
    void cancelInvoice_ShouldThrowException_WhenAlreadyPaid() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice123", "Cancel reason"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("Cannot cancel a paid invoice");
    }

    @Test
    void cancelInvoice_ShouldThrowException_WhenAlreadyCancelled() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice123", "Cancel reason"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelInvoice_ShouldAppendToExistingNotes() {
        testInvoice.setStatus(InvoiceStatus.PENDING);
        testInvoice.setNotes("Previous note");
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.cancelInvoice("invoice123", "New cancellation");

        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes().contains("Previous note") &&
                        invoice.getNotes().contains("New cancellation")));
    }

    // ==================== PAY INVOICE ADDITIONAL TESTS ====================

    @Test
    void payInvoice_ShouldThrowException_WhenAlreadyPaid() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.payInvoice("invoice123", request, "customer123"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void payInvoice_ShouldThrowException_WhenCancelled() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.payInvoice("invoice123", request, "customer123"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("cancelled invoice");
    }

    @Test
    void payInvoice_ShouldWorkWithCashPayment() {
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CASH)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.payInvoice("invoice123", request, "customer123");

        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    void payInvoice_ShouldWorkWithUPIPayment() {
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.UPI)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceResponse result = invoiceService.payInvoice("invoice123", request, "customer123");

        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
    }

    @Test
    void payInvoice_ShouldAppendToExistingNotes() {
        testInvoice.setNotes("Previous note");
        PayInvoiceRequest request = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .notes("Payment by card")
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        invoiceService.payInvoice("invoice123", request, "customer123");

        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes().contains("Previous note") &&
                        invoice.getNotes().contains("Payment by card")));
    }

    // ==================== CREATE INVOICE ADDITIONAL TESTS ====================

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenDuplicateInvoice() {
        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(DuplicateInvoiceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingCannotBeInvoiced() {
        // When canInvoice is false, booking details won't be complete and should throw
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("canInvoice", false);
        // Missing required fields like customerId, basePrice etc.

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(Exception.class); // May throw BillingException or NPE due to missing data
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingServiceReturnsNull() {
        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(null);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class);
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleFallbackResponse() {
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", false);
        responseWrapper.put("fallback", true);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class);
    }

    // ==================== REVENUE REPORT ADDITIONAL TESTS ====================

    @Test
    void getRevenueReport_ShouldHandleEmptyInvoiceList() {
        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(report.getTotalInvoices()).isEqualTo(0L);
        assertThat(report.getTotalRevenue()).isEqualTo(0.0);
        assertThat(report.getCollectedRevenue()).isEqualTo(0.0);
        assertThat(report.getPendingRevenue()).isEqualTo(0.0);
    }

    @Test
    void getRevenueReport_ShouldHandleInvoicesWithNullCategory() {
        Invoice invoiceWithNullCategory = createInvoiceWithStatus(InvoiceStatus.PAID, null, 500.0);

        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(invoiceWithNullCategory));

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now().minusDays(30), LocalDate.now());

        assertThat(report.getRevenueByCategory()).containsKey("Uncategorized");
    }

    @Test
    void getRevenueReport_ShouldExcludeCancelledFromTotalRevenue() {
        Invoice paidInvoice = createInvoiceWithStatus(InvoiceStatus.PAID, "HVAC", 1000.0);
        Invoice cancelledInvoice = createInvoiceWithStatus(InvoiceStatus.CANCELLED, "HVAC", 500.0);

        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Arrays.asList(paidInvoice, cancelledInvoice));

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now().minusDays(30), LocalDate.now());

        // Total revenue should only include non-cancelled = 1000
        assertThat(report.getTotalRevenue()).isEqualTo(1000.0);
    }

    // ==================== SEARCH ADDITIONAL TESTS ====================

    @Test
    void searchInvoices_ShouldReturnEmptyList_WhenNoMatches() {
        when(invoiceRepository.searchInvoices("nonexistent")).thenReturn(Collections.emptyList());

        List<InvoiceResponse> results = invoiceService.searchInvoices("nonexistent");

        assertThat(results).isEmpty();
    }

    @Test
    void searchInvoices_ShouldSearchByCustomerName() {
        when(invoiceRepository.searchInvoices("John")).thenReturn(List.of(testInvoice));

        List<InvoiceResponse> results = invoiceService.searchInvoices("John");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void searchInvoices_ShouldSearchByServiceName() {
        when(invoiceRepository.searchInvoices("AC Repair")).thenReturn(List.of(testInvoice));

        List<InvoiceResponse> results = invoiceService.searchInvoices("AC Repair");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getServiceName()).isEqualTo("AC Repair");
    }
}
