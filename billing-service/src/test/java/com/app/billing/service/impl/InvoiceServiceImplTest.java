package com.app.billing.service.impl;

import com.app.billing.client.BookingServiceClient;
import com.app.billing.dto.request.PayInvoiceRequest;
import com.app.billing.dto.response.InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
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
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private EventPublisherService eventPublisherService;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private Invoice invoice;

    @BeforeEach
    void setUp() {
        invoice = Invoice.builder()
                .id("inv-123")
                .invoiceNumber("INV-2026-00001")
                .bookingId("book-123")
                .customerId("cust-1")
                .totalAmount(100.0)
                .status(InvoiceStatus.PENDING)
                .build();
    }

    @Test
    void createInvoiceFromBooking_Success() {
        // Mock Booking Service Response
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingId", "book-123");
        bookingData.put("basePrice", 100.0);
        bookingData.put("customerId", "cust-1");

        Map<String, Object> responseWrapper = new HashMap<>();
        responseWrapper.put("success", true);
        responseWrapper.put("data", bookingData);

        when(invoiceRepository.existsByBookingId("book-123")).thenReturn(false);
        when(bookingServiceClient.getBookingForInvoice("book-123")).thenReturn(responseWrapper);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

        InvoiceResponse response = invoiceService.createInvoiceFromBooking("book-123", "SYSTEM");

        assertNotNull(response);
        assertEquals("book-123", response.getBookingId());
        verify(eventPublisherService).publishBillingEvent(any());
    }

    @Test
    void createInvoiceFromBooking_Duplicate_ThrowsException() {
        when(invoiceRepository.existsByBookingId("book-123")).thenReturn(true);

        assertThrows(DuplicateInvoiceException.class,
                () -> invoiceService.createInvoiceFromBooking("book-123", "SYSTEM"));
    }

    @Test
    void getInvoiceById_Success() {
        when(invoiceRepository.findById("inv-123")).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.getInvoiceById("inv-123");

        assertEquals("inv-123", response.getId());
    }

    @Test
    void getInvoiceById_NotFound_ThrowsException() {
        when(invoiceRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> invoiceService.getInvoiceById("invalid"));
    }

    @Test
    void payInvoice_Success() {
        PayInvoiceRequest request = new PayInvoiceRequest(PaymentMethod.CARD, "Notes");

        when(invoiceRepository.findById("inv-123")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = invoiceService.payInvoice("inv-123", request, "user");

        assertEquals(InvoiceStatus.PAID, response.getStatus());
        assertEquals(PaymentMethod.CARD, response.getPaymentMethod());
    }

    @Test
    void payInvoice_AlreadyPaid_ThrowsException() {
        invoice.setStatus(InvoiceStatus.PAID);
        PayInvoiceRequest request = new PayInvoiceRequest(PaymentMethod.CARD, "Notes");

        when(invoiceRepository.findById("inv-123")).thenReturn(Optional.of(invoice));

        assertThrows(BillingException.class,
                () -> invoiceService.payInvoice("inv-123", request, "user"));
    }

    @Test
    void cancelInvoice_Success() {
        when(invoiceRepository.findById("inv-123")).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        InvoiceResponse response = invoiceService.cancelInvoice("inv-123", "Reason");

        assertEquals(InvoiceStatus.CANCELLED, response.getStatus());
    }

    @Test
    void getRevenueReport_Success() {
        Invoice paidInvoice = Invoice.builder().totalAmount(100.0).status(InvoiceStatus.PAID).build();
        Invoice pendingInvoice = Invoice.builder().totalAmount(50.0).status(InvoiceStatus.PENDING).build();

        when(invoiceRepository.findByCreatedAtBetween(any(), any()))
                .thenReturn(List.of(paidInvoice, pendingInvoice));

        RevenueReportResponse report = invoiceService.getRevenueReport(LocalDate.now(), LocalDate.now());

        assertEquals(150.0, report.getTotalRevenue()); // Only non-cancelled
        assertEquals(100.0, report.getCollectedRevenue());
        assertEquals(50.0, report.getPendingRevenue());
        assertEquals(2, report.getTotalInvoices());
    }
}