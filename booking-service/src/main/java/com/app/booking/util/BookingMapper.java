package com.app.booking.util;

import com.app.booking. dto.response.BookingResponse;
import com.app.booking.model.Booking;

public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingNumber(booking. getBookingNumber())
                .customerId(booking.getCustomerId())
                .customerName(booking.getCustomerName())
                .customerPhone(booking.getCustomerPhone())
                .customerEmail(booking.getCustomerEmail())
                .serviceId(booking.getServiceId())
                .serviceName(booking.getServiceName())
                .categoryId(booking.getCategoryId())
                .categoryName(booking. getCategoryName())
                .technicianId(booking.getTechnicianId())
                .technicianName(booking.getTechnicianName())
                .technicianPhone(booking.getTechnicianPhone())
                .status(booking.getStatus())
                .priority(booking.getPriority())
                .problemDescription(booking.getProblemDescription())
                .specialInstructions(booking.getSpecialInstructions())
                .scheduledDate(booking.getScheduledDate())
                .estimatedDurationMinutes(booking.getEstimatedDurationMinutes())
                .serviceAddress(booking.getServiceAddress())
                .pricing(booking.getPricing())
                .ratingFeedback(booking.getRatingFeedback())
                .assignedAt(booking.getAssignedAt())
                .confirmedAt(booking.getConfirmedAt())
                .startedAt(booking.getStartedAt())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .cancellation(booking.getCancellation())
                .build();
    }
}