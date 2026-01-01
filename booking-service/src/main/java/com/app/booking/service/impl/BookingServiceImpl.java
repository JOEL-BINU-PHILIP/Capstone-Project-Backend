package com.app.booking.service.impl;

import com.app.booking.dto.request.*;
import com.app.booking.dto.response.BookingResponse;
import com.app.booking.dto.response.BookingStatsResponse;
import com.app.booking.exception.InvalidStateException;
import com.app.booking.exception.ResourceNotFoundException;
import com.app.booking. exception.UnauthorizedException;
import com. app.booking.model.*;
import com.app.booking. repository.BookingRepository;
import com.app.booking.service. BookingService;
import com. app.booking.util.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time. Instant;
import java.time.Year;
import java.util.*;
import java.util.stream. Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    // ==================== CUSTOMER OPERATIONS ====================

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String customerId,
                                         String customerName, String customerEmail, String customerPhone) {

        Booking booking = Booking.builder()
                .bookingNumber(generateBookingNumber())
                .customerId(customerId)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .serviceId(request.getServiceId())
                // Note: serviceName, categoryId, categoryName should ideally be fetched from catalog service
                .serviceName("Service") // Placeholder - integrate with catalog service
                .categoryName("Category") // Placeholder
                .status(BookingStatus. PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : Priority.NORMAL)
                .problemDescription(request.getProblemDescription())
                .imageUrls(request.getImageUrls())
                .scheduledDate(request.getScheduledDate())
                .serviceAddress(AddressDetails.builder()
                        .addressLine1(request.getAddressLine1())
                        .addressLine2(request.getAddressLine2())
                        .city(request.getCity())
                        .state(request.getState())
                        .zipCode(request.getZipCode())
                        .latitude(request.getLatitude())
                        .longitude(request.getLongitude())
                        .build())
                .specialInstructions(request.getSpecialInstructions())
                .otpVerified(false)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: {} by customer: {}", saved.getBookingNumber(), customerId);

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

        return BookingMapper.toResponse(saved);
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
        return bookingRepository. findByTechnicianId(technicianId, pageable)
                .map(BookingMapper::toResponse);
    }

    @Override
    public List<BookingResponse> getTechnicianActiveBookings(String technicianId) {
        List<BookingStatus> activeStatuses = Arrays.asList(
                BookingStatus.ASSIGNED,
                BookingStatus. CONFIRMED,
                BookingStatus.IN_PROGRESS
        );
        return bookingRepository.findByTechnicianIdAndStatusIn(technicianId, activeStatuses)
                .stream()
                .map(BookingMapper::toResponse)
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

        return BookingMapper.toResponse(saved);
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
        booking.setUpdatedAt(Instant. now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} started by technician {}", bookingId, technicianId);

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
        booking.setUpdatedAt(Instant. now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} completed by technician {}", bookingId, technicianId);

        return BookingMapper.toResponse(saved);
    }

    // ==================== MANAGER OPERATIONS ====================

    @Override
    public List<BookingResponse> getPendingBookings() {
        return bookingRepository.findByStatus(BookingStatus.PENDING)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status)
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookingResponse> getAllBookingsPaged(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(BookingMapper::toResponse);
    }

    @Override
    @Transactional
    public BookingResponse assignTechnician(String bookingId, AssignTechnicianRequest request, String managerId) {
        Booking booking = getBookingEntity(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus. REJECTED) {
            throw new InvalidStateException("Can only assign technician to pending or rejected bookings");
        }

        booking.setTechnicianId(request.getTechnicianId());
        booking.setTechnicianName(request.getTechnicianName());
        booking.setTechnicianPhone(request.getTechnicianPhone());
        booking.setAssignedBy(managerId);
        booking.setAssignedAt(Instant.now());
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} assigned to technician {} by manager {}", bookingId, request.getTechnicianId(), managerId);

        return BookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse reassignTechnician(String bookingId, AssignTechnicianRequest request, String managerId) {
        Booking booking = getBookingEntity(bookingId);

        List<BookingStatus> reassignableStatuses = Arrays.asList(
                BookingStatus. ASSIGNED,
                BookingStatus.CONFIRMED,
                BookingStatus. REJECTED
        );

        if (!reassignableStatuses.contains(booking.getStatus())) {
            throw new InvalidStateException("Cannot reassign technician at this stage");
        }

        booking. setTechnicianId(request.getTechnicianId());
        booking.setTechnicianName(request.getTechnicianName());
        booking.setTechnicianPhone(request.getTechnicianPhone());
        booking.setAssignedBy(managerId);
        booking.setAssignedAt(Instant.now());
        booking.setStatus(BookingStatus.ASSIGNED);
        booking.setConfirmedAt(null);
        booking.setUpdatedAt(Instant.now());

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} reassigned to technician {} by manager {}", bookingId, request.getTechnicianId(), managerId);

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
                BookingStatus. ASSIGNED,
                BookingStatus. CONFIRMED
        );
        if (!reschedulableStatuses.contains(booking.getStatus())) {
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
        return String.format("%06d", new Random().nextInt(1000000));
    }
}