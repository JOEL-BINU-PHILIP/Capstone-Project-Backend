package com.app.billing.service.impl;

import com.app.billing.dto.request.CreateInvoiceRequest;
import com.app.billing.dto.request.PayInvoiceRequest;
import com. app.billing.dto.response. InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com.app.billing.event.BillingEvent;
import com.app. billing.event.EventType;
import com.app.billing.exception.BillingException;
import com. app.billing.exception.DuplicateInvoiceException;
import com.app.billing. exception.ResourceNotFoundException;
import com.app.billing.model.Invoice;
import com.app.billing.model.InvoiceStatus;
import com.app.billing. repository.InvoiceRepository;
import com.app.billing.service. EventPublisherService;
import com.app.billing.service. InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java. time.LocalDate;
import java.time. Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EventPublisherService eventPublisherService;

    private static final Double DEFAULT_TAX_PERCENTAGE = 18.0;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String createdBy) {

        // Check if invoice already exists for this booking
        if (invoiceRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateInvoiceException("Invoice already exists for booking: " + request.getBookingId());
        }

        // Calculate pricing
        double basePrice = request.getBasePrice();
        double taxPercentage = request.getTaxPercentage() != null ? request.getTaxPercentage() : DEFAULT_TAX_PERCENTAGE;
        double discountPercentage = request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0;

        double taxAmount = basePrice * (taxPercentage / 100);
        double discountAmount = basePrice * (discountPercentage / 100);
        double totalAmount = basePrice + taxAmount - discountAmount;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .bookingId(request.getBookingId())
                .bookingNumber(request.getBookingNumber())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request. getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .serviceId(request.getServiceId())
                .serviceName(request. getServiceName())
                .categoryName(request.getCategoryName())
                .technicianId(request.getTechnicianId())
                .technicianName(request.getTechnicianName())
                .basePrice(basePrice)
                .taxPercentage(taxPercentage)
                .taxAmount(taxAmount)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .currency("INR")
                .status(InvoiceStatus.PENDING)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(7))
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created:  {} for booking: {}, total: {}",
                saved.getInvoiceNumber(), request.getBookingId(), totalAmount);

        // Publish INVOICE_GENERATED event
        publishInvoiceGeneratedEvent(saved);

        return toResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(String invoiceId) {
        return toResponse(getInvoiceEntity(invoiceId));
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found:  " + invoiceNumber));
        return toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByBookingId(String bookingId) {
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for booking: " + bookingId));
        return toResponse(invoice);
    }

    @Override
    public List<InvoiceResponse> getCustomerInvoices(String customerId) {
        return invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<InvoiceResponse> getCustomerInvoicesPaged(String customerId, Pageable pageable) {
        return invoiceRepository.findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status)
                .stream()
                .map(this::toResponse)
                .collect(Collectors. toList());
    }

    @Override
    public Page<InvoiceResponse> getAllInvoicesPaged(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse payInvoice(String invoiceId, PayInvoiceRequest request, String paidBy) {
        Invoice invoice = getInvoiceEntity(invoiceId);

        // Validate
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice is already paid");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException("Cannot pay a cancelled invoice");
        }

        // Mark as paid
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaidAt(Instant.now());
        invoice.setPaidBy(paidBy);

        if (request.getNotes() != null) {
            String existingNotes = invoice.getNotes() != null ? invoice.getNotes() + "\n" : "";
            invoice.setNotes(existingNotes + "Payment note: " + request.getNotes());
        }

        invoice. setUpdatedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} marked as PAID by {}, method: {}",
                invoice.getInvoiceNumber(), paidBy, request.getPaymentMethod());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(String invoiceId, String reason) {
        Invoice invoice = getInvoiceEntity(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Cannot cancel a paid invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException("Invoice is already cancelled");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        String existingNotes = invoice.getNotes() != null ? invoice.getNotes() + "\n" : "";
        invoice.setNotes(existingNotes + "Cancellation reason: " + reason);
        invoice.setUpdatedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} cancelled.  Reason: {}", invoice.getInvoiceNumber(), reason);

        return toResponse(saved);
    }

    @Override
    public RevenueReportResponse getRevenueReport(LocalDate startDate, LocalDate endDate) {
        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Invoice> invoices = invoiceRepository.findByCreatedAtBetween(startInstant, endInstant);

        // Calculate totals using Java Streams
        double totalRevenue = invoices.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        double collectedRevenue = invoices.stream()
                .filter(inv -> inv. getStatus() == InvoiceStatus.PAID)
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        double pendingRevenue = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PENDING)
                .mapToDouble(Invoice::getTotalAmount)
                .sum();

        // Count by status
        Map<String, Long> invoicesByStatus = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getStatus().name(),
                        Collectors.counting()
                ));

        // Revenue by category (only paid invoices)
        Map<String, Double> revenueByCategory = invoices. stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .collect(Collectors.groupingBy(
                        inv -> inv.getCategoryName() != null ? inv.getCategoryName() : "Uncategorized",
                        Collectors.summingDouble(Invoice:: getTotalAmount)
                ));

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .collectedRevenue(collectedRevenue)
                .pendingRevenue(pendingRevenue)
                .totalInvoices((long) invoices.size())
                .paidInvoices(invoicesByStatus.getOrDefault(InvoiceStatus.PAID. name(), 0L))
                .pendingInvoices(invoicesByStatus.getOrDefault(InvoiceStatus.PENDING. name(), 0L))
                .cancelledInvoices(invoicesByStatus.getOrDefault(InvoiceStatus.CANCELLED.name(), 0L))
                .invoicesByStatus(invoicesByStatus)
                .revenueByCategory(revenueByCategory)
                .periodStart(startDate. toString())
                .periodEnd(endDate.toString())
                .build();
    }

    @Override
    public List<InvoiceResponse> searchInvoices(String query) {
        return invoiceRepository.searchInvoices(query)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private Invoice getInvoiceEntity(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        String random = String.format("%05d", new Random().nextInt(100000));
        return "INV-" + year + "-" + random;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .bookingId(invoice.getBookingId())
                .bookingNumber(invoice.getBookingNumber())
                .customerId(invoice. getCustomerId())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .customerPhone(invoice.getCustomerPhone())
                .serviceId(invoice.getServiceId())
                .serviceName(invoice.getServiceName())
                .categoryName(invoice.getCategoryName())
                .technicianId(invoice.getTechnicianId())
                .technicianName(invoice.getTechnicianName())
                .basePrice(invoice.getBasePrice())
                .taxPercentage(invoice.getTaxPercentage())
                .taxAmount(invoice.getTaxAmount())
                .discountPercentage(invoice.getDiscountPercentage())
                .discountAmount(invoice.getDiscountAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .isPaid(invoice.getStatus() == InvoiceStatus.PAID)
                .paymentMethod(invoice.getPaymentMethod())
                .paidAt(invoice.getPaidAt())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    // ==================== EVENT PUBLISHING ====================

    private void publishInvoiceGeneratedEvent(Invoice invoice) {
        try {
            BillingEvent event = BillingEvent.builder()
                    . eventType(EventType.INVOICE_GENERATED)
                    . userId(invoice.getCustomerId())
                    .userEmail(invoice.getCustomerEmail())
                    .userName(invoice.getCustomerName())
                    .userRole("CUSTOMER")
                    .invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .amount(invoice.getTotalAmount())
                    .currency(invoice.getCurrency())
                    .bookingId(invoice.getBookingId())
                    .bookingNumber(invoice.getBookingNumber())
                    .build();

            eventPublisherService.publishBillingEvent(event);

        } catch (Exception e) {
            log.error("Failed to publish invoice generated event for invoice {}: {}",
                    invoice.getInvoiceNumber(), e.getMessage());
            // Don't fail the main operation if event publishing fails
        }
    }
}