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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

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

    // ==================== CREATE INVOICE FROM BOOKING TESTS ====================

    @Test
    void createInvoiceFromBooking_ShouldCreateInvoice_WhenBookingValid() {
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

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });
        doNothing().when(eventPublisherService).publishBillingEvent(any(BillingEvent.class));

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo("booking123");
        assertThat(result.getCustomerId()).isEqualTo("customer123");
        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(invoiceRepository).save(any(Invoice.class));
        verify(eventPublisherService).publishBillingEvent(any(BillingEvent.class));
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenDuplicateInvoice() {
        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(DuplicateInvoiceException.class)
                .hasMessageContaining("Invoice already exists for booking");

        verify(bookingServiceClient, never()).getBookingForInvoice(anyString());
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingServiceFails() {
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", false);
        responseWrapper.put("fallback", true);
        responseWrapper.put("message", "Service unavailable");

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("Booking service unavailable");
    }

    @Test
    void createInvoiceFromBooking_ShouldThrowException_WhenBookingNotCompleted() {
        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", false);
        responseWrapper.put("message", "Booking not completed");

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);

        assertThatThrownBy(() -> invoiceService.createInvoiceFromBooking("booking123", "SYSTEM"))
                .isInstanceOf(BillingException.class);
    }

    @Test
    void createInvoiceFromBooking_ShouldHandleNullTaxPercentage_WhenNotProvided() {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "booking123");
        bookingData.put("customerId", "customer123");
        bookingData.put("basePrice", 1000.0);
        // No taxPercentage provided - will use default value via toDouble conversion

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("booking123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("booking123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            invoice.setId("invoice123");
            return invoice;
        });

        InvoiceResponse result = invoiceService.createInvoiceFromBooking("booking123", "SYSTEM");

        assertThat(result).isNotNull();
        // When taxPercentage is null, toDouble returns 0.0, but DEFAULT_TAX_PERCENTAGE (18.0) is used
        verify(invoiceRepository).save(any(Invoice.class));
    }

    // ==================== GET INVOICE BY ID TESTS ====================

    @Test
    void getInvoiceById_ShouldReturnInvoice_WhenFound() {
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceById("invoice123");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("invoice123");
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2026-00001");
        verify(invoiceRepository).findById("invoice123");
    }

    @Test
    void getInvoiceById_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }

    // ==================== GET INVOICE BY NUMBER TESTS ====================

    @Test
    void getInvoiceByNumber_ShouldReturnInvoice_WhenFound() {
        when(invoiceRepository.findByInvoiceNumber("INV-2026-00001")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceByNumber("INV-2026-00001");

        assertThat(result).isNotNull();
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-2026-00001");
        verify(invoiceRepository).findByInvoiceNumber("INV-2026-00001");
    }

    @Test
    void getInvoiceByNumber_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findByInvoiceNumber("INVALID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByNumber("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found");
    }

    // ==================== GET INVOICE BY BOOKING ID TESTS ====================

    @Test
    void getInvoiceByBookingId_ShouldReturnInvoice_WhenFound() {
        when(invoiceRepository.findByBookingId("booking123")).thenReturn(Optional.of(testInvoice));

        InvoiceResponse result = invoiceService.getInvoiceByBookingId("booking123");

        assertThat(result).isNotNull();
        assertThat(result.getBookingId()).isEqualTo("booking123");
        verify(invoiceRepository).findByBookingId("booking123");
    }

    @Test
    void getInvoiceByBookingId_ShouldThrowException_WhenNotFound() {
        when(invoiceRepository.findByBookingId("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.getInvoiceByBookingId("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found for booking");
    }

    // ==================== GET INVOICES LIST TESTS ====================

    @Test
    void getInvoices_ShouldReturnAllInvoices_WhenManagerWithNoFilters() {
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findAll(any(Pageable.class))).thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                null, null, "manager123", true, PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findAll(any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldFilterByCustomerId_WhenProvided() {
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByCustomerId(eq("customer123"), any(Pageable.class))).thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                "customer123", null, "manager123", true, PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByCustomerId(eq("customer123"), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldFilterByStatus_WhenProvided() {
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByStatus(eq(InvoiceStatus.PENDING), any(Pageable.class))).thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                null, InvoiceStatus.PENDING, "manager123", true, PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByStatus(eq(InvoiceStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getInvoices_ShouldFilterByBothCustomerAndStatus_WhenBothProvided() {
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByCustomerIdAndStatus(
                eq("customer123"), eq(InvoiceStatus.PENDING), any(Pageable.class)
        )).thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                "customer123", InvoiceStatus.PENDING, "manager123", true, PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByCustomerIdAndStatus(
                eq("customer123"), eq(InvoiceStatus.PENDING), any(Pageable.class)
        );
    }

    @Test
    void getInvoices_ShouldRestrictToOwnInvoices_WhenNotManager() {
        Page<Invoice> invoicePage = new PageImpl<>(List.of(testInvoice), PageRequest.of(0, 20), 1);
        when(invoiceRepository.findByCustomerId(eq("customer123"), any(Pageable.class))).thenReturn(invoicePage);

        Page<InvoiceResponse> result = invoiceService.getInvoices(
                null, null, "customer123", false, PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).hasSize(1);
        verify(invoiceRepository).findByCustomerId(eq("customer123"), any(Pageable.class));
    }

    // ==================== PAY INVOICE TESTS ====================

    @Test
    void payInvoice_ShouldMarkAsPaid_WhenPendingInvoice() {
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .notes("Paid via web")
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse result = invoiceService.payInvoice("invoice123", payRequest, "customer123");

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getStatus() == InvoiceStatus.PAID &&
                invoice.getPaymentMethod() == PaymentMethod.CARD &&
                invoice.getPaidBy().equals("customer123") &&
                invoice.getPaidAt() != null
        ));
    }

    @Test
    void payInvoice_ShouldAppendPaymentNotes_WhenProvided() {
        testInvoice.setNotes("Existing notes");
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.UPI)
                .notes("Payment note")
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.payInvoice("invoice123", payRequest, "customer123");

        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes().contains("Existing notes") &&
                invoice.getNotes().contains("Payment note")
        ));
    }

    @Test
    void payInvoice_ShouldThrowException_WhenAlreadyPaid() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.payInvoice("invoice123", payRequest, "customer123"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already paid");
    }

    @Test
    void payInvoice_ShouldThrowException_WhenCancelled() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.payInvoice("invoice123", payRequest, "customer123"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("cancelled invoice");
    }

    @Test
    void payInvoice_ShouldThrowException_WhenInvoiceNotFound() {
        PayInvoiceRequest payRequest = PayInvoiceRequest.builder()
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(invoiceRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.payInvoice("nonexistent", payRequest, "customer123"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== CANCEL INVOICE TESTS ====================

    @Test
    void cancelInvoice_ShouldMarkAsCancelled_WhenPendingInvoice() {
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse result = invoiceService.cancelInvoice("invoice123", "Customer request");

        assertThat(result.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getStatus() == InvoiceStatus.CANCELLED &&
                invoice.getNotes().contains("Customer request")
        ));
    }

    @Test
    void cancelInvoice_ShouldThrowException_WhenAlreadyPaid() {
        testInvoice.setStatus(InvoiceStatus.PAID);
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice123", "Test reason"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("Cannot cancel a paid invoice");
    }

    @Test
    void cancelInvoice_ShouldThrowException_WhenAlreadyCancelled() {
        testInvoice.setStatus(InvoiceStatus.CANCELLED);
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));

        assertThatThrownBy(() -> invoiceService.cancelInvoice("invoice123", "Test reason"))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelInvoice_ShouldAppendReasonToExistingNotes() {
        testInvoice.setNotes("Previous notes");
        when(invoiceRepository.findById("invoice123")).thenReturn(Optional.of(testInvoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        invoiceService.cancelInvoice("invoice123", "Customer request");

        verify(invoiceRepository).save(argThat(invoice ->
                invoice.getNotes().contains("Previous notes") &&
                invoice.getNotes().contains("Customer request")
        ));
    }

    // ==================== REVENUE REPORT TESTS ====================

    @Test
    void getRevenueReport_ShouldCalculateCorrectTotals() {
        Invoice paidInvoice1 = Invoice.builder()
                .totalAmount(1000.0)
                .status(InvoiceStatus.PAID)
                .categoryName("HVAC")
                .createdAt(Instant.now())
                .build();

        Invoice paidInvoice2 = Invoice.builder()
                .totalAmount(500.0)
                .status(InvoiceStatus.PAID)
                .categoryName("Plumbing")
                .createdAt(Instant.now())
                .build();

        Invoice pendingInvoice = Invoice.builder()
                .totalAmount(300.0)
                .status(InvoiceStatus.PENDING)
                .categoryName("HVAC")
                .createdAt(Instant.now())
                .build();

        Invoice cancelledInvoice = Invoice.builder()
                .totalAmount(200.0)
                .status(InvoiceStatus.CANCELLED)
                .categoryName("Electrical")
                .createdAt(Instant.now())
                .build();

        List<Invoice> invoices = Arrays.asList(paidInvoice1, paidInvoice2, pendingInvoice, cancelledInvoice);
        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(invoices);

        RevenueReportResponse report = invoiceService.getRevenueReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(report.getTotalRevenue()).isEqualTo(1800.0); // All except cancelled
        assertThat(report.getCollectedRevenue()).isEqualTo(1500.0); // Only paid
        assertThat(report.getPendingRevenue()).isEqualTo(300.0); // Only pending
        assertThat(report.getTotalInvoices()).isEqualTo(4L);
        assertThat(report.getPaidInvoices()).isEqualTo(2L);
        assertThat(report.getPendingInvoices()).isEqualTo(1L);
        assertThat(report.getCancelledInvoices()).isEqualTo(1L);
    }

    @Test
    void getRevenueReport_ShouldCalculateRevenueByCategory() {
        Invoice hvacInvoice1 = Invoice.builder()
                .totalAmount(1000.0)
                .status(InvoiceStatus.PAID)
                .categoryName("HVAC")
                .createdAt(Instant.now())
                .build();

        Invoice hvacInvoice2 = Invoice.builder()
                .totalAmount(500.0)
                .status(InvoiceStatus.PAID)
                .categoryName("HVAC")
                .createdAt(Instant.now())
                .build();

        Invoice plumbingInvoice = Invoice.builder()
                .totalAmount(800.0)
                .status(InvoiceStatus.PAID)
                .categoryName("Plumbing")
                .createdAt(Instant.now())
                .build();

        List<Invoice> invoices = Arrays.asList(hvacInvoice1, hvacInvoice2, plumbingInvoice);
        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(invoices);

        RevenueReportResponse report = invoiceService.getRevenueReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(report.getRevenueByCategory()).containsEntry("HVAC", 1500.0);
        assertThat(report.getRevenueByCategory()).containsEntry("Plumbing", 800.0);
    }

    @Test
    void getRevenueReport_ShouldHandleEmptyResults() {
        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        RevenueReportResponse report = invoiceService.getRevenueReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(report.getTotalRevenue()).isEqualTo(0.0);
        assertThat(report.getCollectedRevenue()).isEqualTo(0.0);
        assertThat(report.getPendingRevenue()).isEqualTo(0.0);
        assertThat(report.getTotalInvoices()).isEqualTo(0L);
    }

    @Test
    void getRevenueReport_ShouldHandleNullCategoryName() {
        Invoice invoiceWithNullCategory = Invoice.builder()
                .totalAmount(500.0)
                .status(InvoiceStatus.PAID)
                .categoryName(null)
                .createdAt(Instant.now())
                .build();

        when(invoiceRepository.findByCreatedAtBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(invoiceWithNullCategory));

        RevenueReportResponse report = invoiceService.getRevenueReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31)
        );

        assertThat(report.getRevenueByCategory()).containsEntry("Uncategorized", 500.0);
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void searchInvoices_ShouldReturnMatchingInvoices() {
        List<Invoice> searchResults = List.of(testInvoice);
        when(invoiceRepository.searchInvoices("John")).thenReturn(searchResults);

        List<InvoiceResponse> result = invoiceService.searchInvoices("John");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("John Doe");
        verify(invoiceRepository).searchInvoices("John");
    }

    @Test
    void searchInvoices_ShouldReturnEmptyList_WhenNoMatches() {
        when(invoiceRepository.searchInvoices("NonExistent")).thenReturn(Collections.emptyList());

        List<InvoiceResponse> result = invoiceService.searchInvoices("NonExistent");

        assertThat(result).isEmpty();
    }
}

