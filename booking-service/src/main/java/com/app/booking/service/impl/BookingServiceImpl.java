package com.app.booking.service.impl;

import com.app.booking.client.AuthServiceClient;
import com.app.booking.client.CatalogServiceClient;
import com.app.booking.dto.external.ApiResponseWrapper;
import com.app.booking.dto.request.*;
import com.app.booking. dto.response.BookingResponse;
import com.app.booking.dto.response.BookingStatsResponse;
import com.app.booking.event.BookingEvent;
import com.app.booking.event.EventType;
import com.app.booking.exception.BookingException;
import com.app.booking.exception.InvalidStateException;
import com.app. booking.exception.ResourceNotFoundException;
import com.app.booking.exception.UnauthorizedException;
import com. app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service. BookingService;
import com. app.booking.service.EventPublisherService;
import com.app.booking.util.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.stream. Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventPublisherService eventPublisherService;

    // Feign Clients
    private final AuthServiceClient authServiceClient;
    private final CatalogServiceClient catalogServiceClient;

    // ==================== CUSTOMER OPERATIONS ====================

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String customerId,
                                         String customerName, String customerEmail, String customerPhone) {

        // 1. Fetch service details from Catalog Service
        ServiceDetails serviceDetails = fetchServiceDetails(request.getServiceId());

        // 2. Build pricing details
        PricingDetails pricing = buildPricingDetails(serviceDetails);

        // 3. Create booking with enriched data
        Booking booking = Booking.builder()
                .bookingNumber(generateBookingNumber())
                .customerId(customerId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                // Service info from Catalog Service
                .serviceId(request.getServiceId())
                .serviceName(serviceDetails.getServiceName())
                .categoryName(serviceDetails.getCategoryName())
                // Pricing from Catalog Service
                .pricing(pricing)
                .status(BookingStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .problemDescription(request.getProblemDescription())
                .imageUrls(request.getImageUrls())
                .scheduledDate(request.getScheduledDate())
                .serviceAddress(AddressDetails.builder()
                        .addressLine1(request.getAddressLine1())
                        . addressLine2(request.getAddressLine2())
                        . city(request.getCity())
                        .state(request.getState())
                        .zipCode(request.getZipCode())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build())
                .specialInstructions(request.getSpecialInstructions())
                .otpVerified(false)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: {} by customer: {} for service: {}",
                saved.getBookingNumber(), customerId, serviceDetails.getServiceName());

        // Publish BOOKING_CREATED event
        publishEvent(saved, EventType.BOOKING_CREATED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    public List<BookingResponse> getCustomerBookings(String customerId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookingResponse> getCustomerBookingsPaged(String customerId, Pageable pageable) {
        return bookingRepository.findByCustomerId(customerId, pageable)
                .map(BookingMapper:: toResponse);
    }

    @Override
    @Transactional
    public BookingResponse rescheduleBooking(String bookingId, RescheduleBookingRequest request, String customerId) {
        Booking booking = getBookingEntity(bookingId);

        validateCustomerOwnership(booking, customerId);
        validateReschedulable(booking);

        booking.setScheduledDate(request.getNewScheduledDate());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} rescheduled by customer {}", bookingId, customerId);

        // Publish BOOKING_RESCHEDULED event
        publishEvent(saved, EventType.BOOKING_RESCHEDULED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByCustomer(String bookingId, CancelBookingRequest request, String customerId) {
        Booking booking = getBookingEntity(bookingId);

        validateCustomerOwnership(booking, customerId);
        validateCancellable(booking);

        booking.setStatus(BookingStatus. CANCELLED);
        booking.setCancellation(CancellationDetails.builder()
                .cancelledBy(customerId)
                .cancelledByRole("CUSTOMER")
                .cancellationReason(request.getReason())
                .cancelledAt(Instant.now())
                .build());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled by customer {}", bookingId, customerId);

        // Publish BOOKING_CANCELLED event
        publishEvent(saved, EventType.BOOKING_CANCELLED, request.getReason(), null);

        return BookingMapper. toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse rateBooking(String bookingId, RateBookingRequest request, String customerId) {
        Booking booking = getBookingEntity(bookingId);

        validateCustomerOwnership(booking, customerId);

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidStateException("Can only rate completed bookings");
        }

        if (booking.getRatingFeedback() != null) {
            throw new InvalidStateException("Booking has already been rated");
        }

        booking.setRatingFeedback(RatingFeedback. builder()
                .rating(request.getRating())
                .feedback(request.getFeedback())
                .ratedAt(Instant.now())
                .build());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} rated with {} stars by customer {}", bookingId, request.getRating(), customerId);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public String generateCompletionOtp(String bookingId, String customerId) {
        Booking booking = getBookingEntity(bookingId);

        validateCustomerOwnership(booking, customerId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidStateException("OTP can only be generated when service is in progress");
        }

        String otp = generateOtp();
        booking.setCompletionOtp(otp);
        bookingRepository.save(booking);

        log.info("OTP generated for booking {}", bookingId);
        return otp;
    }

    // ==================== TECHNICIAN OPERATIONS ====================

    @Override
    public List<BookingResponse> getTechnicianBookings(String technicianId) {
        return bookingRepository.findByTechnicianIdOrderByScheduledDateAsc(technicianId)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors. toList());
    }

    @Override
    public Page<BookingResponse> getTechnicianBookingsPaged(String technicianId, Pageable pageable) {
        return bookingRepository.findByTechnicianId(technicianId, pageable)
                .map(BookingMapper::toResponse);
    }

    @Override
    public List<BookingResponse> getTechnicianActiveBookings(String technicianId) {
        List<BookingStatus> activeStatuses = Arrays.asList(
                BookingStatus.ASSIGNED,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );
        return bookingRepository.findByTechnicianIdAndStatusIn(technicianId, activeStatuses)
                .stream()
                .map(BookingMapper:: toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(String bookingId, String technicianId) {
        Booking booking = getBookingEntity(bookingId);

        validateTechnicianOwnership(booking, technicianId);

        if (booking.getStatus() != BookingStatus.ASSIGNED) {
            throw new InvalidStateException("Can only confirm assigned bookings");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository. save(booking);
        log.info("Booking {} confirmed by technician {}", bookingId, technicianId);

        // Publish BOOKING_CONFIRMED event
        publishEvent(saved, EventType.BOOKING_CONFIRMED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse rejectBooking(String bookingId, String technicianId, String reason) {
        Booking booking = getBookingEntity(bookingId);

        validateTechnicianOwnership(booking, technicianId);

        if (booking.getStatus() != BookingStatus.ASSIGNED) {
            throw new InvalidStateException("Can only reject assigned bookings");
        }

        booking. setStatus(BookingStatus. REJECTED);
        booking.setTechnicianId(null);
        booking.setTechnicianName(null);
        booking.setTechnicianPhone(null);
        booking.setTechnicianNotes(reason);
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} rejected by technician {}", bookingId, technicianId);

        // Publish BOOKING_REJECTED event
        publishEvent(saved, EventType.BOOKING_REJECTED, null, reason);

        return BookingMapper. toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse startService(String bookingId, String technicianId) {
        Booking booking = getBookingEntity(bookingId);

        validateTechnicianOwnership(booking, technicianId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidStateException("Can only start confirmed bookings");
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedAt(Instant.now());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository. save(booking);
        log.info("Booking {} started by technician {}", bookingId, technicianId);

        // Publish SERVICE_STARTED event
        publishEvent(saved, EventType.SERVICE_STARTED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse completeService(String bookingId, CompleteBookingRequest request, String technicianId) {
        Booking booking = getBookingEntity(bookingId);

        validateTechnicianOwnership(booking, technicianId);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new InvalidStateException("Can only complete in-progress bookings");
        }

        // Verify OTP
        if (booking.getCompletionOtp() == null || !booking.getCompletionOtp().equals(request.getOtp())) {
            throw new InvalidStateException("Invalid completion OTP");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setOtpVerified(true);
        booking.setCompletedAt(Instant.now());
        booking.setTechnicianNotes(request.getTechnicianNotes());
        booking.setCompletionImageUrls(request.getCompletionImageUrls());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} completed by technician {}", bookingId, technicianId);

        // Increment technician's completed jobs via Auth Service
        incrementTechnicianJobs(technicianId);

        // Publish SERVICE_COMPLETED event
        publishEvent(saved, EventType.SERVICE_COMPLETED, null, null);

        return BookingMapper.toResponse(saved);
    }

    // ==================== MANAGER OPERATIONS ====================

    @Override
    public List<BookingResponse> getPendingBookings() {
        return bookingRepository. findByStatus(BookingStatus.PENDING)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors. toList());
    }

    @Override
    public Page<BookingResponse> getAllBookingsPaged(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(BookingMapper:: toResponse);
    }

    @Override
    @Transactional
    public BookingResponse assignTechnician(String bookingId, AssignTechnicianRequest request, String managerId) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus. REJECTED) {
            throw new InvalidStateException("Can only assign technician to pending or rejected bookings");
        }

        // Validate and fetch technician details from Auth Service
        TechnicianDetails technicianDetails = fetchAndValidateTechnician(request.getTechnicianId());

        booking.setTechnicianId(request.getTechnicianId());
        booking.setTechnicianName(technicianDetails.getFullName());
        booking.setTechnicianPhone(technicianDetails.getPhoneNumber());
        booking.setAssignedBy(managerId);
        booking.setAssignedAt(Instant.now());
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} assigned to technician {} ({}) by manager {}",
                bookingId, technicianDetails. getFullName(), request.getTechnicianId(), managerId);

        // Publish TECHNICIAN_ASSIGNED event
        publishEvent(saved, EventType. TECHNICIAN_ASSIGNED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse reassignTechnician(String bookingId, AssignTechnicianRequest request, String managerId) {
        Booking booking = getBookingEntity(bookingId);

        List<BookingStatus> reassignableStatuses = Arrays.asList(
                BookingStatus. ASSIGNED,
                BookingStatus. CONFIRMED,
                BookingStatus. REJECTED
        );

        if (!reassignableStatuses.contains(booking.getStatus())) {
            throw new InvalidStateException("Cannot reassign technician at this stage");
        }

        // Validate and fetch technician details from Auth Service
        TechnicianDetails technicianDetails = fetchAndValidateTechnician(request.getTechnicianId());

        booking.setTechnicianId(request.getTechnicianId());
        booking.setTechnicianName(technicianDetails. getFullName());
        booking.setTechnicianPhone(technicianDetails.getPhoneNumber());
        booking.setAssignedBy(managerId);
        booking.setAssignedAt(Instant.now());
        booking.setStatus(BookingStatus. ASSIGNED);
        booking.setConfirmedAt(null);
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} reassigned to technician {} ({}) by manager {}",
                bookingId, technicianDetails.getFullName(), request.getTechnicianId(), managerId);

        // Publish TECHNICIAN_ASSIGNED event
        publishEvent(saved, EventType.TECHNICIAN_ASSIGNED, null, null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByManager(String bookingId, CancelBookingRequest request, String managerId) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStateException("Cannot cancel completed or already cancelled bookings");
        }

        booking.setStatus(BookingStatus. CANCELLED);
        booking.setCancellation(CancellationDetails.builder()
                .cancelledBy(managerId)
                .cancelledByRole("SERVICE_MANAGER")
                .cancellationReason(request.getReason())
                .cancelledAt(Instant.now())
                .build());
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled by manager {}", bookingId, managerId);

        // Publish BOOKING_CANCELLED event
        publishEvent(saved, EventType. BOOKING_CANCELLED, request. getReason(), null);

        return BookingMapper.toResponse(saved);
    }

    @Override
    public BookingStatsResponse getBookingStats() {
        Map<String, Long> statusCounts = new HashMap<>();
        for (BookingStatus status : BookingStatus.values()) {
            statusCounts.put(status.name(), bookingRepository.countByStatus(status));
        }

        return BookingStatsResponse.builder()
                .totalBookings(bookingRepository.count())
                .pendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING))
                .assignedBookings(bookingRepository. countByStatus(BookingStatus. ASSIGNED))
                .inProgressBookings(bookingRepository. countByStatus(BookingStatus. IN_PROGRESS))
                .completedBookings(bookingRepository.countByStatus(BookingStatus.COMPLETED))
                .cancelledBookings(bookingRepository. countByStatus(BookingStatus. CANCELLED))
                .bookingsByStatus(statusCounts)
                .build();
    }

    @Override
    public List<BookingResponse> searchBookings(String query) {
        return bookingRepository. searchBookings(query)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== COMMON ====================

    @Override
    public BookingResponse getBookingById(String bookingId) {
        return BookingMapper.toResponse(getBookingEntity(bookingId));
    }

    @Override
    public BookingResponse getBookingByNumber(String bookingNumber) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found:  " + bookingNumber));
        return BookingMapper.toResponse(booking);
    }

    // ==================== FEIGN CLIENT METHODS ====================

    /**
     * Fetch service details from Catalog Service
     */
    private ServiceDetails fetchServiceDetails(String serviceId) {
        try {
            log.debug("Fetching service details from Catalog Service for serviceId: {}", serviceId);

            Map<String, Object> response = catalogServiceClient.getServiceForBooking(serviceId);

            if (response == null) {
                log.warn("Catalog service returned null response for serviceId: {}", serviceId);
                return getDefaultServiceDetails(serviceId);
            }

            Boolean success = (Boolean) response.get("success");
            if (success == null || !success) {
                String message = (String) response.get("message");
                log.warn("Failed to fetch service details:  {}", message);

                // Check if this is a fallback response
                if (Boolean.TRUE.equals(response.get("fallback"))) {
                    log.warn("Using fallback - Catalog service unavailable");
                    return getDefaultServiceDetails(serviceId);
                }

                throw new BookingException("Service not found or not available: " + serviceId);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            if (data == null) {
                return getDefaultServiceDetails(serviceId);
            }

            return ServiceDetails.builder()
                    . serviceId(serviceId)
                    .serviceName((String) data.get("serviceName"))
                    .categoryName((String) data.get("categoryName"))
                    .basePrice(toDouble(data.get("basePrice")))
                    .taxPercentage(toDouble(data. get("taxPercentage")))
                    .taxAmount(toDouble(data.get("taxAmount")))
                    . discountPercentage(toDouble(data.get("discountPercentage")))
                    .discountAmount(toDouble(data. get("discountAmount")))
                    .finalPrice(toDouble(data.get("finalPrice")))
                    .currency((String) data.getOrDefault("currency", "INR"))
                    .estimatedDurationMinutes(toInteger(data.get("estimatedDurationMinutes")))
                    .build();

        } catch (BookingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching service details from Catalog Service:  {}", e.getMessage());
            return getDefaultServiceDetails(serviceId);
        }
    }

    /**
     * Fetch and validate technician from Auth Service
     */
    private TechnicianDetails fetchAndValidateTechnician(String technicianUserId) {
        try {
            log.debug("Validating technician from Auth Service:  {}", technicianUserId);

            // First validate the technician
            ApiResponseWrapper<Map<String, Object>> validationResponse =
                    authServiceClient.validateTechnician(technicianUserId);

            if (validationResponse == null || !validationResponse.isSuccess()) {
                log.warn("Technician validation failed for:  {}", technicianUserId);
                throw new BookingException("Unable to validate technician:  " + technicianUserId);
            }

            Map<String, Object> validationData = validationResponse.getData();

            // Check if fallback response
            if (Boolean.TRUE.equals(validationData.get("fallback"))) {
                log. warn("Auth service unavailable - cannot validate technician");
                throw new BookingException("Auth service unavailable.  Please try again later.");
            }

            Boolean canAssign = (Boolean) validationData.get("canAssign");
            if (canAssign == null || !canAssign) {
                Boolean exists = (Boolean) validationData.get("exists");
                Boolean approved = (Boolean) validationData.get("approved");
                Boolean available = (Boolean) validationData.get("available");

                if (! Boolean.TRUE.equals(exists)) {
                    throw new BookingException("Technician not found:  " + technicianUserId);
                }
                if (!Boolean.TRUE.equals(approved)) {
                    throw new BookingException("Technician is not approved: " + technicianUserId);
                }
                if (!Boolean.TRUE.equals(available)) {
                    throw new BookingException("Technician is not available: " + technicianUserId);
                }
                throw new BookingException("Technician cannot be assigned: " + technicianUserId);
            }

            // Fetch full technician details
            ApiResponseWrapper<Map<String, Object>> technicianResponse =
                    authServiceClient. getTechnicianByUserId(technicianUserId);

            if (technicianResponse == null || !technicianResponse.isSuccess() || technicianResponse.getData() == null) {
                log.warn("Could not fetch technician details, using basic info");
                return TechnicianDetails.builder()
                        .userId(technicianUserId)
                        .fullName("Technician")
                        . phoneNumber(null)
                        .build();
            }

            Map<String, Object> techData = technicianResponse.getData();

            return TechnicianDetails.builder()
                    .userId(technicianUserId)
                    . fullName((String) techData.getOrDefault("fullName", "Technician"))
                    .email((String) techData.get("email"))
                    .phoneNumber((String) techData.get("phoneNumber"))
                    . build();

        } catch (BookingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating technician from Auth Service: {}", e.getMessage());
            throw new BookingException("Failed to validate technician.  Please try again.");
        }
    }

    /**
     * Increment technician's completed job count via Auth Service
     */
    private void incrementTechnicianJobs(String technicianUserId) {
        try {
            log.debug("Incrementing job count for technician: {}", technicianUserId);

            ApiResponseWrapper<Void> response = authServiceClient.incrementTechnicianJobs(technicianUserId);

            if (response != null && response.isSuccess()) {
                log.info("Successfully incremented job count for technician:  {}", technicianUserId);
            } else {
                log.warn("Failed to increment job count for technician: {}. Will retry later.", technicianUserId);
                // Don't throw exception - this is not critical for booking completion
            }
        } catch (Exception e) {
            log.error("Error incrementing technician jobs: {}. Will retry later.", e.getMessage());
            // Don't throw exception - this is not critical for booking completion
        }
    }

    // ==================== HELPER CLASSES ====================

    @lombok.Builder
    @lombok.Data
    private static class ServiceDetails {
        private String serviceId;
        private String serviceName;
        private String categoryName;
        private Double basePrice;
        private Double taxPercentage;
        private Double taxAmount;
        private Double discountPercentage;
        private Double discountAmount;
        private Double finalPrice;
        private String currency;
        private Integer estimatedDurationMinutes;
    }

    @lombok.Builder
    @lombok.Data
    private static class TechnicianDetails {
        private String userId;
        private String fullName;
        private String email;
        private String phoneNumber;
    }

    // ==================== HELPER METHODS ====================

    private Booking getBookingEntity(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
    }

    private void validateCustomerOwnership(Booking booking, String customerId) {
        if (!booking.getCustomerId().equals(customerId)) {
            throw new UnauthorizedException("You don't have access to this booking");
        }
    }

    private void validateTechnicianOwnership(Booking booking, String technicianId) {
        if (booking.getTechnicianId() == null || !booking.getTechnicianId().equals(technicianId)) {
            throw new UnauthorizedException("This booking is not assigned to you");
        }
    }

    private void validateReschedulable(Booking booking) {
        List<BookingStatus> reschedulableStatuses = Arrays. asList(
                BookingStatus.PENDING,
                BookingStatus.ASSIGNED,
                BookingStatus.CONFIRMED
        );
        if (!reschedulableStatuses. contains(booking.getStatus())) {
            throw new InvalidStateException("Cannot reschedule booking at this stage");
        }
    }

    private void validateCancellable(Booking booking) {
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) {
            throw new InvalidStateException("Cannot cancel booking once service has started");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidStateException("Booking is already completed or cancelled");
        }
    }

    private String generateBookingNumber() {
        String year = String.valueOf(Year.now().getValue());
        String random = String.format("%05d", new Random().nextInt(100000));
        return "BK-" + year + "-" + random;
    }

    private String generateOtp() {
        return String. format("%06d", new Random().nextInt(1000000));
    }

    private PricingDetails buildPricingDetails(ServiceDetails serviceDetails) {
        return PricingDetails.builder()
                .basePrice(serviceDetails.getBasePrice())
                .taxPercentage(serviceDetails.getTaxPercentage())
                .taxAmount(serviceDetails.getTaxAmount())
                .discountPercentage(serviceDetails.getDiscountPercentage())
                .discountAmount(serviceDetails.getDiscountAmount())
                .finalPrice(serviceDetails.getFinalPrice())
                .currency(serviceDetails.getCurrency() != null ? serviceDetails.getCurrency() : "INR")
                .build();
    }

    private ServiceDetails getDefaultServiceDetails(String serviceId) {
        log.warn("Using default service details for serviceId: {}", serviceId);
        return ServiceDetails.builder()
                .serviceId(serviceId)
                .serviceName("Service")
                .categoryName("General")
                .basePrice(0.0)
                .taxPercentage(18.0)
                .taxAmount(0.0)
                .discountPercentage(0.0)
                .discountAmount(0.0)
                .finalPrice(0.0)
                .currency("INR")
                .estimatedDurationMinutes(60)
                .build();
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

    private Integer toInteger(Object value) {
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // ==================== EVENT PUBLISHING ====================

    private void publishEvent(Booking booking, EventType eventType,
                              String cancellationReason, String rejectionReason) {
        try {
            BookingEvent event = BookingEvent. builder()
                    .eventType(eventType)
                    .userId(booking.getCustomerId())
                    .userEmail(booking.getCustomerEmail())
                    .userName(booking.getCustomerName())
                    .userRole("CUSTOMER")
                    .bookingId(booking.getId())
                    .bookingNumber(booking.getBookingNumber())
                    .bookingStatus(booking.getStatus().name())
                    .serviceId(booking.getServiceId())
                    .serviceName(booking.getServiceName())
                    .categoryName(booking.getCategoryName())
                    .technicianId(booking.getTechnicianId())
                    . technicianName(booking.getTechnicianName())
                    .technicianPhone(booking.getTechnicianPhone())
                    .scheduledDate(booking.getScheduledDate() != null ?
                            booking.getScheduledDate().toString() : null)
                    . cancellationReason(cancellationReason)
                    .rejectionReason(rejectionReason)
                    .build();

            eventPublisherService.publishBookingEvent(event);

        } catch (Exception e) {
            log.error("Failed to publish event {} for booking {}: {}",
                    eventType, booking.getBookingNumber(), e.getMessage());
        }
    }

    // ==================== READ - WITH ACCESS CHECK ====================

    @Override
    public BookingResponse getBookingByIdWithAccessCheck(String bookingId, String userId, List<String> roles) {
        Booking booking = getBookingEntity(bookingId);
        validateAccessToBooking(booking, userId, roles);
        return BookingMapper. toResponse(booking);
    }

    @Override
    public BookingResponse getBookingByNumberWithAccessCheck(String bookingNumber, String userId, List<String> roles) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found:  " + bookingNumber));
        validateAccessToBooking(booking, userId, roles);
        return BookingMapper.toResponse(booking);
    }

    // ==================== READ - ALL (MANAGER) ====================

    @Override
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== ACCESS VALIDATION HELPER ====================

    /**
     * Validate if user has access to view/modify the booking
     * - MANAGER/ADMIN:  Can access all bookings
     * - TECHNICIAN: Can access only assigned bookings
     * - CUSTOMER: Can access only own bookings
     */
    private void validateAccessToBooking(Booking booking, String userId, List<String> roles) {
        // Managers and Admins can access all bookings
        if (roles.contains("SERVICE_MANAGER") || roles.contains("ADMIN")) {
            log.debug("Manager/Admin access granted for booking {}", booking.getId());
            return;
        }

        // Technicians can only access bookings assigned to them
        if (roles. contains("TECHNICIAN")) {
            if (booking.getTechnicianId() == null || ! booking.getTechnicianId().equals(userId)) {
                log.warn("Technician {} denied access to booking {}", userId, booking.getId());
                throw new UnauthorizedException("You don't have access to this booking");
            }
            log.debug("Technician access granted for booking {}", booking.getId());
            return;
        }

        // Customers can only access their own bookings
        if (roles.contains("CUSTOMER")) {
            if (! booking.getCustomerId().equals(userId)) {
                log.warn("Customer {} denied access to booking {}", userId, booking.getId());
                throw new UnauthorizedException("You don't have access to this booking");
            }
            log. debug("Customer access granted for booking {}", booking.getId());
            return;
        }

        // Default: deny access
        log.warn("User {} with roles {} denied access to booking {}", userId, roles, booking.getId());
        throw new UnauthorizedException("Access denied");
    }
}