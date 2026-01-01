package com. app.booking.service;

import com.app.booking.dto. request.*;
import com.app.booking. dto.response.BookingResponse;
import com.app.booking. dto.response.BookingStatsResponse;
import com.app. booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {

    // ==================== CUSTOMER OPERATIONS ====================

    BookingResponse createBooking(CreateBookingRequest request, String customerId,
                                  String customerName, String customerEmail, String customerPhone);

    List<BookingResponse> getCustomerBookings(String customerId);

    Page<BookingResponse> getCustomerBookingsPaged(String customerId, Pageable pageable);

    BookingResponse rescheduleBooking(String bookingId, RescheduleBookingRequest request, String customerId);

    BookingResponse cancelBookingByCustomer(String bookingId, CancelBookingRequest request, String customerId);

    BookingResponse rateBooking(String bookingId, RateBookingRequest request, String customerId);

    String generateCompletionOtp(String bookingId, String customerId);

    // ==================== TECHNICIAN OPERATIONS ====================

    List<BookingResponse> getTechnicianBookings(String technicianId);

    Page<BookingResponse> getTechnicianBookingsPaged(String technicianId, Pageable pageable);

    List<BookingResponse> getTechnicianActiveBookings(String technicianId);

    BookingResponse confirmBooking(String bookingId, String technicianId);

    BookingResponse rejectBooking(String bookingId, String technicianId, String reason);

    BookingResponse startService(String bookingId, String technicianId);

    BookingResponse completeService(String bookingId, CompleteBookingRequest request, String technicianId);

    // ==================== MANAGER OPERATIONS ====================

    List<BookingResponse> getPendingBookings();

    List<BookingResponse> getBookingsByStatus(BookingStatus status);

    Page<BookingResponse> getAllBookingsPaged(Pageable pageable);

    BookingResponse assignTechnician(String bookingId, AssignTechnicianRequest request, String managerId);

    BookingResponse reassignTechnician(String bookingId, AssignTechnicianRequest request, String managerId);

    BookingResponse cancelBookingByManager(String bookingId, CancelBookingRequest request, String managerId);

    BookingStatsResponse getBookingStats();

    List<BookingResponse> searchBookings(String query);

    // ==================== COMMON ====================

    BookingResponse getBookingById(String bookingId);

    BookingResponse getBookingByNumber(String bookingNumber);
}