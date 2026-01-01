package com.app.billing. service.impl;

import com. app.billing.dto.request. CreateInvoiceRequest;
import com.app.billing.dto. request.UpdateInvoiceRequest;
import com.app.billing. dto.response.InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com. app.billing.exception.BillingException;
import com. app.billing.exception.DuplicateInvoiceException;
import com.app.billing.exception.ResourceNotFoundException;
import com.app. billing.model.Invoice;
import com.app.billing.model.InvoiceStatus;
import com.app.billing. repository.InvoiceRepository;
import com.app.billing.service. InvoiceService;
import com.app.billing.util.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction. annotation.Transactional;

import java.time. Instant;
import java.time. LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.*;
import java.util.stream. Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String createdBy) {

        // Check if invoice already exists for this booking
        if (invoiceRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateInvoiceException("Invoice already exists for booking: " + request.getBookingId());
        }

        // Calculate tax and discount amounts
        double taxAmount = request.getSubtotal() * (request.getTaxPercentage() != null ? request.getTaxPercentage() / 100 : 0);
        double discountAmount = request.getSubtotal() * (request.getDiscountPercentage() != null ? request.getDiscountPercentage() / 100 : 0);

        // Build line items
        List<Invoice.LineItem> lineItems = new ArrayList<>();
        if (request.getLineItems() != null) {
            lineItems = request.getLineItems().stream()
                    .map(item -> Invoice.LineItem.builder()
                            .description(item.getDescription())
                            .quantity(item.getQuantity() != null ? item.getQuantity() : 1)
                            .unitPrice(item.getUnitPrice())
                            .amount(item.getUnitPrice() * (item.getQuantity() != null ? item.getQuantity() : 1))
                            .build())
                    .collect(Collectors.toList());
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .bookingId(request.getBookingId())
                .bookingNumber(request.getBookingNumber())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request. getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .serviceId(request.getServiceId())
                .serviceName(request.getServiceName())
                .categoryName(request.getCategoryName())
                .technicianId(request.getTechnicianId())
                .technicianName(request.getTechnicianName())
                .lineItems(lineItems)
                .subtotal(request.getSubtotal())
                .taxPercentage(request.getTaxPercentage() != null ? request.getTaxPercentage() : 0.0)
                .taxAmount(taxAmount)
                .discountPercentage(request.getDiscountPercentage() != null ? request.getDiscountPercentage() : 0.0)
                .discountAmount(discountAmount)
                .totalAmount(request.getTotalAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .amountPaid(0.0)
                .balanceDue(request.getTotalAmount())
                .status(InvoiceStatus.PENDING)
                .invoiceDate(LocalDate.now())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusDays(7))
                .notes(request.getNotes())
                .createdBy(createdBy)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created:  {} for booking: {}", saved.getInvoiceNumber(), request.getBookingId());

        return InvoiceMapper.toResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(String invoiceId) {
        return InvoiceMapper.toResponse(getInvoiceEntity(invoiceId));
    }

    @Override
    public InvoiceResponse getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found:  " + invoiceNumber));
        return InvoiceMapper.toResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByBookingId(String bookingId) {
        Invoice invoice = invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for booking: " + bookingId));
        return InvoiceMapper.toResponse(invoice);
    }

    @Override
    public List<InvoiceResponse> getCustomerInvoices(String customerId) {
        return invoiceRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(InvoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<InvoiceResponse> getCustomerInvoicesPaged(String customerId, Pageable pageable) {
        return invoiceRepository. findByCustomerId(customerId, pageable)
                .map(InvoiceMapper::toResponse);
    }

    @Override
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status)
                .stream()
                .map(InvoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<InvoiceResponse> getAllInvoicesPaged(Pageable pageable) {
        return invoiceRepository.findAll(pageable)
                .map(InvoiceMapper::toResponse);
    }

    @Override
    public List<InvoiceResponse> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDate.now())
                .stream()
                .map(InvoiceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoice(String invoiceId, UpdateInvoiceRequest request) {
        Invoice invoice = getInvoiceEntity(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException("Cannot update paid or cancelled invoice");
        }

        if (request.getDiscountPercentage() != null) {
            invoice.setDiscountPercentage(request.getDiscountPercentage());
            double discountAmount = invoice.getSubtotal() * request.getDiscountPercentage() / 100;
            invoice. setDiscountAmount(discountAmount);
            double newTotal = invoice.getSubtotal() + invoice.getTaxAmount() - discountAmount;
            invoice. setTotalAmount(newTotal);
            invoice.setBalanceDue(newTotal - invoice.getAmountPaid());
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request. getNotes());
        }

        if (request.getTermsAndConditions() != null) {
            invoice.setTermsAndConditions(request.getTermsAndConditions());
        }

        invoice.setUpdatedAt(Instant.now());
        Invoice saved = invoiceRepository.save(invoice);

        log.info("Invoice updated: {}", invoiceId);
        return InvoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse updateInvoiceStatus(String invoiceId, InvoiceStatus status) {
        Invoice invoice = getInvoiceEntity(invoiceId);
        invoice.setStatus(status);
        invoice.setUpdatedAt(Instant.now());

        if (status == InvoiceStatus. PAID) {
            invoice.setPaidAt(Instant.now());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} status updated to: {}", invoiceId, status);

        return InvoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(String invoiceId, String reason) {
        Invoice invoice = getInvoiceEntity(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Cannot cancel a paid invoice.  Use refund instead.");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setNotes(invoice.getNotes() != null ?
                invoice.getNotes() + "\nCancellation reason: " + reason :
                "Cancellation reason: " + reason);
        invoice.setUpdatedAt(Instant.now());

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} cancelled.  Reason: {}", invoiceId, reason);

        return InvoiceMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void updateInvoicePayment(String invoiceId, Double amountPaid) {
        Invoice invoice = getInvoiceEntity(invoiceId);

        double newAmountPaid = invoice.getAmountPaid() + amountPaid;
        invoice.setAmountPaid(newAmountPaid);
        invoice.setBalanceDue(invoice.getTotalAmount() - newAmountPaid);

        // Update status based on payment
        if (newAmountPaid >= invoice.getTotalAmount()) {
            invoice. setStatus(InvoiceStatus. PAID);
            invoice.setPaidAt(Instant.now());
            invoice.setBalanceDue(0.0);
        } else if (newAmountPaid > 0) {
            invoice.setStatus(InvoiceStatus. PARTIALLY_PAID);
        }

        invoice.setUpdatedAt(Instant.now());
        invoiceRepository.save(invoice);

        log.info("Invoice {} payment updated. Amount paid: {}, Balance due: {}",
                invoiceId, newAmountPaid, invoice.getBalanceDue());
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

        double totalPaid = invoices.stream()
                .mapToDouble(Invoice::getAmountPaid)
                .sum();

        double totalPending = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PENDING || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .mapToDouble(Invoice::getBalanceDue)
                .sum();

        double totalOverdue = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE)
                .mapToDouble(Invoice::getBalanceDue)
                .sum();

        // Count by status
        Map<String, Long> statusCounts = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getStatus().name(),
                        Collectors.counting()
                ));

        // Revenue by category
        Map<String, Double> revenueByCategory = invoices. stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        inv -> inv.getCategoryName() != null ? inv.getCategoryName() : "Uncategorized",
                        Collectors.summingDouble(Invoice:: getTotalAmount)
                ));

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalPaid(totalPaid)
                .totalPending(totalPending)
                .totalOverdue(totalOverdue)
                .totalInvoices((long) invoices.size())
                .paidInvoices(statusCounts.getOrDefault(InvoiceStatus.PAID. name(), 0L))
                .pendingInvoices(statusCounts.getOrDefault(InvoiceStatus.PENDING.name(), 0L))
                .overdueInvoices(statusCounts. getOrDefault(InvoiceStatus.OVERDUE.name(), 0L))
                .revenueByCategory(revenueByCategory)
                .periodStart(startDate. toString())
                .periodEnd(endDate.toString())
                .build();
    }

    @Override
    public List<InvoiceResponse> searchInvoices(String query) {
        return invoiceRepository.searchInvoices(query)
                .stream()
                .map(InvoiceMapper::toResponse)
                .collect(Collectors. toList());
    }

    @Override
    @Scheduled(cron = "0 0 1 * * *") // Run daily at 1 AM
    @Transactional
    public void markOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(LocalDate. now());

        for (Invoice invoice : overdueInvoices) {
            invoice.setStatus(InvoiceStatus. OVERDUE);
            invoice.setUpdatedAt(Instant.now());
        }

        if (! overdueInvoices.isEmpty()) {
            invoiceRepository.saveAll(overdueInvoices);
            log.info("Marked {} invoices as overdue", overdueInvoices.size());
        }
    }

    // Helper methods
    private Invoice getInvoiceEntity(String invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found:  " + invoiceId));
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        String random = String.format("%05d", new Random().nextInt(100000));
        return "INV-" + year + "-" + random;
    }
}