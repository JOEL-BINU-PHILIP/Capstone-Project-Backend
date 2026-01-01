package com.app.booking.service;

import com.app.booking.event.BookingEvent;

public interface EventPublisherService {
    void publishBookingEvent(BookingEvent event);
}