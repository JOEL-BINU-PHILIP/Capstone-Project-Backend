package com.app.booking.repository;

import com.app.booking.model. Booking;
import com.app. booking.model.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time. Instant;
import java.time.LocalDateTime;
import java. util.List;
import java. util.Optional;

public interface BookingRepository extends MongoRepository<Booking, String> {

    // Find by booking number
    Optional<Booking> findByBookingNumber(String bookingNumber);

    // Customer bookings
    List<Booking> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    Page<Booking> findByCustomerId(String customerId, Pageable pageable);
    List<Booking> findByCustomerIdAndStatus(String customerId, BookingStatus status);

    // Technician bookings
    List<Booking> findByTechnicianIdOrderByScheduledDateAsc(String technicianId);
    Page<Booking> findByTechnicianId(String technicianId, Pageable pageable);
    List<Booking> findByTechnicianIdAndStatus(String technicianId, BookingStatus status);
    List<Booking> findByTechnicianIdAndStatusIn(String technicianId, List<BookingStatus> statuses);

    // Manager queries
    List<Booking> findByStatus(BookingStatus status);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    List<Booking> findByStatusIn(List<BookingStatus> statuses);

    // By service/category
    List<Booking> findByServiceId(String serviceId);
    List<Booking> findByCategoryId(String categoryId);

    // Date range queries
    @Query("{'scheduledDate': {$gte: ?0, $lte: ?1}}")
    List<Booking> findByScheduledDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("{'createdAt': {$gte:  ?0, $lte: ? 1}}")
    List<Booking> findByCreatedAtBetween(Instant start, Instant end);

    // Statistics
    long countByStatus(BookingStatus status);
    long countByCustomerId(String customerId);
    long countByTechnicianId(String technicianId);
    long countByTechnicianIdAndStatus(String technicianId, BookingStatus status);

    // Search
    @Query("{'$or':  [{'bookingNumber': {$regex: ? 0, $options: 'i'}}, {'customerName': {$regex: ?0, $options: 'i'}}, {'serviceName': {$regex: ?0, $options: 'i'}}]}")
    List<Booking> searchBookings(String query);
}