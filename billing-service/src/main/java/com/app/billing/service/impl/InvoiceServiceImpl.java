package com.app.billing. service.impl;

import com. app.billing.client.BookingServiceClient;
import com.app. billing.dto.request.CreateInvoiceRequest;
import com.app.billing.dto.request.PayInvoiceRequest;
import com. app.billing.dto.response. InvoiceResponse;
import com.app.billing.dto.response.RevenueReportResponse;
import com.app.billing.event.BillingEvent;
import com. app.billing.event.EventType;
import com.app.billing.exception.BillingException;
import com. app.billing.exception.DuplicateInvoiceException;
import com.app.billing.exception.ResourceNotFoundException;
import com.app.billing.model. Invoice;
import com.app.billing.model.InvoiceStatus;
import com.app.billing.repository.InvoiceRepository;
import com.app.billing.service. EventPublisherService;
import com.app.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain. Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import java. time. Instant;
import java.time.LocalDate;
import java.time.Year;
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

    // Feign Client
    private final BookingServiceClient bookingServiceClient;

    private static final Double DEFAULT_TAX_PERCENTAGE = 18.0;

    @Override
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String createdBy) {

        // Check if invoice already exists for this booking
        if (invoiceRepository.existsByBookingId(request.getBookingId())) {
            throw new DuplicateInvoiceException("Invoice already exists for booking: " + request.getBookingId());
        }

        // Fetch booking details from Booking Service (optional enrichment)
        BookingDetails bookingDetails = fetchBookingDetails(request.getBookingId());

        // Use request data, enriched with booking details where missing
        String customerName = request.getCustomerName() != null ?
                request.getCustomerName() : bookingDetails.getCustomerName();
        String customerEmail = request. getCustomerEmail() != null ?
                request.getCustomerEmail() : bookingDetails.getCustomerEmail();
        String customerPhone = request.getCustomerPhone() != null ?
                request.getCustomerPhone() : bookingDetails.getCustomerPhone();
        String serviceName = request.getServiceName() != null ?
                request. getServiceName() : bookingDetails.getServiceName();
        String categoryName = request.getCategoryName() != null ?
                request.getCategoryName() : bookingDetails.getCategoryName();
        String technicianId = request.getTechnicianId() != null ?
                request.getTechnicianId() : bookingDetails.getTechnicianId();
        String technicianName = request.getTechnicianName() != null ?
                request.getTechnicianName() : bookingDetails.getTechnicianName();

        // Calculate pricing
        double basePrice = request.getBasePrice() != null ?
                request.getBasePrice() : bookingDetails.getBasePrice();
        double taxPercentage = request.getTaxPercentage() != null ?
                request.getTaxPercentage() :
                (bookingDetails.getTaxPercentage() != null ? bookingDetails.getTaxPercentage() : DEFAULT_TAX_PERCENTAGE);
        double discountPercentage = request.getDiscountPercentage() != null ?
                request.getDiscountPercentage() :
                (bookingDetails.getDiscountPercentage() != null ? bookingDetails.getDiscountPercentage() : 0.0);

        double taxAmount = basePrice * (taxPercentage / 100);
        double discountAmount = basePrice * (discountPercentage / 100);
        double totalAmount = basePrice + taxAmount - discountAmount;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .bookingId(request.getBookingId())
                .bookingNumber(request.getBookingNumber() != null ?
                        request.getBookingNumber() : bookingDetails.getBookingNumber())
                .customerId(request.getCustomerId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .serviceId(request.getServiceId())
                .serviceName(serviceName)
                .categoryName(categoryName)
                .technicianId(technicianId)
                .technicianName(technicianName)
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
        log.info("Invoice created: {} for booking: {}, total: {}",
                saved.getInvoiceNumber(), request.getBookingId(), totalAmount);

        // Publish INVOICE_GENERATED event
        publishInvoiceGeneratedEvent(saved);

        return toResponse(saved);
    }

    /**
     * Create invoice directly from booking ID (fetches all data from Booking Service)
     */
    public InvoiceResponse createInvoiceFromBooking(String bookingId, String createdBy) {

        // Check if invoice already exists
        if (invoiceRepository.existsByBookingId(bookingId)) {
            throw new DuplicateInvoiceException("Invoice already exists for booking: " + bookingId);
        }

        // Fetch complete booking details from Booking Service
        BookingDetails bookingDetails = fetchBookingDetailsForInvoice(bookingId);

        if (bookingDetails == null || ! bookingDetails.isCanInvoice()) {
            throw new BillingException("Booking cannot be invoiced.  Booking must be completed.");
        }

        // Calculate amounts
        double basePrice = bookingDetails.getBasePrice() != null ? bookingDetails.getBasePrice() : 0.0;
        double taxPercentage = bookingDetails.getTaxPercentage() != null ? bookingDetails.getTaxPercentage() : DEFAULT_TAX_PERCENTAGE;
        double discountPercentage = bookingDetails.getDiscountPercentage() != null ? bookingDetails.getDiscountPercentage() : 0.0;

        double taxAmount = basePrice * (taxPercentage / 100);
        double discountAmount = basePrice * (discountPercentage / 100);
        double totalAmount = basePrice + taxAmount - discountAmount;

        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .bookingId(bookingId)
                .bookingNumber(bookingDetails.getBookingNumber())
                .customerId(bookingDetails. getCustomerId())
                .customerName(bookingDetails.getCustomerName())
                .customerEmail(bookingDetails.getCustomerEmail())
                .customerPhone(bookingDetails.getCustomerPhone())
                .serviceId(bookingDetails.getServiceId())
                .serviceName(bookingDetails.getServiceName())
                .categoryName(bookingDetails.getCategoryName())
                .technicianId(bookingDetails.getTechnicianId())
                .technicianName(bookingDetails.getTechnicianName())
                .basePrice(basePrice)
                .taxPercentage(taxPercentage)
                .taxAmount(taxAmount)
                .discountPercentage(discountPercentage)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .currency("INR")
                .status(InvoiceStatus.PENDING)
                .invoiceDate(LocalDate. now())
                .dueDate(LocalDate.now().plusDays(7))
                .createdBy(createdBy)
                .build();

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice created from booking: {} -> Invoice: {}, total: {}",
                bookingId, saved.getInvoiceNumber(), totalAmount);

        // Publish INVOICE_GENERATED event
        publishInvoiceGeneratedEvent(saved);

        return toResponse(saved);
    }

    // ==================== FEIGN CLIENT METHODS ====================

    /**
     * Fetch booking details from Booking Service
     */
    private BookingDetails fetchBookingDetails(String bookingId) {
        try {
            log.debug("Fetching booking details from Booking Service for bookingId:  {}", bookingId);

            Map<String, Object> response = bookingServiceClient.getBookingById(bookingId);

            if (response == null) {
                log.warn("Booking service returned null response for bookingId:  {}", bookingId);
                return getDefaultBookingDetails(bookingId);
            }

            Boolean success = (Boolean) response.get("success");
            if (success == null || !success) {
                // Check if fallback
                if (Boolean.TRUE.equals(response.get("fallback"))) {
                    log.warn("Booking service unavailable - using default details");
                    return getDefaultBookingDetails(bookingId);
                }
                log.warn("Failed to fetch booking:  {}", response.get("message"));
                return getDefaultBookingDetails(bookingId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data == null) {
                return getDefaultBookingDetails(bookingId);
            }

            return mapToBookingDetails(data);

        } catch (Exception e) {
            log.error("Error fetching booking details: {}", e.getMessage());
            return getDefaultBookingDetails(bookingId);
        }
    }

    /**
     * Fetch booking details specifically for invoice creation
     */
    private BookingDetails fetchBookingDetailsForInvoice(String bookingId) {
        try {
            log. debug("Fetching booking for invoice from Booking Service:  {}", bookingId);

            Map<String, Object> response = bookingServiceClient.getBookingForInvoice(bookingId);

            if (response == null) {
                log.warn("Booking service returned null for invoice request");
                return null;
            }

            Boolean success = (Boolean) response.get("success");
            if (success == null || !success) {
                String message = (String) response.get("message");
                log.warn("Cannot create invoice: {}", message);

                if (Boolean.TRUE.equals(response.get("fallback"))) {
                    throw new BillingException("Booking service unavailable.  Please try again later.");
                }

                throw new BillingException(message != null ? message : "Booking not found or not completed");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data == null) {
                throw new BillingException("No booking data returned");
            }

            BookingDetails details = mapToBookingDetails(data);
            details.setCanInvoice(true);
            return details;

        } catch (BillingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching booking for invoice:  {}", e.getMessage());
            throw new BillingException("Failed to fetch booking details.  Please try again.");
        }
    }

    private BookingDetails mapToBookingDetails(Map<String, Object> data) {
        BookingDetails details = new BookingDetails();

        details.setBookingId((String) data.get("bookingId"));
        details.setBookingNumber((String) data.get("bookingNumber"));
        details.setCustomerId((String) data.get("customerId"));
        details.setCustomerName((String) data.get("customerName"));
        details.setCustomerEmail((String) data.get("customerEmail"));
        details.setCustomerPhone((String) data.get("customerPhone"));
        details.setServiceId((String) data.get("serviceId"));
        details.setServiceName((String) data.get("serviceName"));
        details.setCategoryName((String) data.get("categoryName"));
        details.setTechnicianId((String) data.get("technicianId"));
        details.setTechnicianName((String) data.get("technicianName"));
        details.setBasePrice(toDouble(data.get("basePrice")));
        details.setTaxPercentage(toDouble(data.get("taxPercentage")));
        details.setTaxAmount(toDouble(data. get("taxAmount")));
        details.setDiscountPercentage(toDouble(data.get("discountPercentage")));
        details.setDiscountAmount(toDouble(data.get("discountAmount")));
        details.setFinalPrice(toDouble(data.get("finalPrice")));
        details.setCurrency((String) data.getOrDefault("currency", "INR"));

        return details;
    }

    private BookingDetails getDefaultBookingDetails(String bookingId) {
        log.warn("Using default booking details for bookingId:  {}", bookingId);
        BookingDetails details = new BookingDetails();
        details.setBookingId(bookingId);
        details.setBasePrice(0.0);
        details.setTaxPercentage(DEFAULT_TAX_PERCENTAGE);
        details.setDiscountPercentage(0.0);
        details.setCurrency("INR");
        return details;
    }

    // ==================== HELPER CLASS ====================

    @lombok.Data
    private static class BookingDetails {
        private String bookingId;
        private String bookingNumber;
        private String customerId;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String serviceId;
        private String serviceName;
        private String categoryName;
        private String technicianId;
        private String technicianName;
        private Double basePrice;
        private Double taxPercentage;
        private Double taxAmount;
        private Double discountPercentage;
        private Double discountAmount;
        private Double finalPrice;
        private String currency;
        private boolean canInvoice;
    }

    // ==================== EXISTING METHODS (unchanged) ====================

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
                .collect(Collectors.toList());
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

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice is already paid");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException("Cannot pay a cancelled invoice");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setPaidAt(Instant.now());
        invoice.setPaidBy(paidBy);

        if (request.getNotes() != null) {
            String existingNotes = invoice.getNotes() != null ? invoice.getNotes() + "\n" : "";
            invoice.setNotes(existingNotes + "Payment note: " + request.getNotes());
        }

        invoice. setUpdatedAt(Instant.now());

        Invoice saved = invoiceRepository. save(invoice);
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

        List<Invoice> invoices = invoiceRepository. findByCreatedAtBetween(startInstant, endInstant);

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

        Map<String, Long> invoicesByStatus = invoices.stream()
                .collect(Collectors.groupingBy(
                        inv -> inv.getStatus().name(),
                        Collectors.counting()
                ));

        Map<String, Double> revenueByCategory = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .collect(Collectors.groupingBy(
                        inv -> inv.getCategoryName() != null ? inv.getCategoryName() : "Uncategorized",
                        Collectors.summingDouble(Invoice::getTotalAmount)
                ));

        return RevenueReportResponse.builder()
                .totalRevenue(totalRevenue)
                .collectedRevenue(collectedRevenue)
                .pendingRevenue(pendingRevenue)
                .totalInvoices((long) invoices.size())
                .paidInvoices(invoicesByStatus.getOrDefault(InvoiceStatus.PAID. name(), 0L))
                .pendingInvoices(invoicesByStatus.getOrDefault(InvoiceStatus.PENDING.name(), 0L))
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
        return invoiceRepository. findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
    }

    private String generateInvoiceNumber() {
        String year = String.valueOf(Year.now().getValue());
        String random = String.format("%05d", new Random().nextInt(100000));
        return "INV-" + year + "-" + random;
    }

    private Double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long) return ((Long) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .bookingId(invoice.getBookingId())
                .bookingNumber(invoice.getBookingNumber())
                .customerId(invoice.getCustomerId())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .customerPhone(invoice. getCustomerPhone())
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
                .notes(invoice. getNotes())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    // ==================== EVENT PUBLISHING ====================

    private void publishInvoiceGeneratedEvent(Invoice invoice) {
        try {
            BillingEvent event = BillingEvent.builder()
                    .eventType(EventType.INVOICE_GENERATED)
                    .userId(invoice.getCustomerId())
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
            log.error("Failed to publish invoice generated event for invoice {}:  {}",
                    invoice.getInvoiceNumber(), e.getMessage());
        }
    }
}