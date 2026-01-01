package com.app.notification.listener;

import com.app.notification.config.RabbitMQConfig;
import com.app.notification. event.BillingEvent;
import com. app.notification.event.BookingEvent;
import com.app. notification.event.EventType;
import com.app.notification.model. Notification;
import com.app. notification.model.NotificationStatus;
import com.app.notification.model.NotificationType;
import com.app.notification.repository.NotificationRepository;
import com.app.notification.service.EmailService;
import com.fasterxml.jackson.databind. JsonNode;
import com.fasterxml.jackson.databind. ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleEvent(String messageJson) {
        try {
            log.info("Received event: {}", messageJson);

            // Parse JSON to get event type
            JsonNode jsonNode = objectMapper.readTree(messageJson);
            String eventTypeStr = jsonNode.get("eventType").asText();
            EventType eventType = EventType.valueOf(eventTypeStr);

            // Route based on event type
            switch (eventType) {
                case BOOKING_CREATED:
                case TECHNICIAN_ASSIGNED:
                case BOOKING_CONFIRMED:
                case BOOKING_REJECTED:
                case SERVICE_STARTED:
                case SERVICE_COMPLETED:
                case BOOKING_CANCELLED:
                case BOOKING_RESCHEDULED:
                    BookingEvent bookingEvent = objectMapper.readValue(messageJson, BookingEvent.class);
                    handleBookingEvent(bookingEvent);
                    break;

                case INVOICE_GENERATED:
                    BillingEvent billingEvent = objectMapper.readValue(messageJson, BillingEvent.class);
                    handleBillingEvent(billingEvent);
                    break;

                default:
                    log. warn("Unknown event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    private void handleBookingEvent(BookingEvent event) {
        log.info("Processing booking event: {} for booking:  {}",
                event.getEventType(), event.getBookingNumber());

        String title;
        String message;
        NotificationType notificationType;

        switch (event.getEventType()) {
            case BOOKING_CREATED:
                notificationType = NotificationType. BOOKING_CREATED;
                title = "Booking Confirmed - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, your booking %s for %s has been created successfully.  " +
                                "We will assign a technician shortly and notify you.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber(),
                        event.getServiceName() != null ? event.getServiceName() : "the service"
                );
                break;

            case TECHNICIAN_ASSIGNED:
                notificationType = NotificationType.TECHNICIAN_ASSIGNED;
                title = "Technician Assigned - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, %s has been assigned to your booking %s. " +
                                "Contact:  %s.  The technician will confirm the schedule shortly.",
                        getDisplayName(event.getUserName()),
                        event.getTechnicianName() != null ? event.getTechnicianName() : "A technician",
                        event. getBookingNumber(),
                        event.getTechnicianPhone() != null ? event.getTechnicianPhone() : "Will be shared soon"
                );
                break;

            case BOOKING_CONFIRMED:
                notificationType = NotificationType.BOOKING_CONFIRMED;
                title = "Booking Confirmed by Technician - " + event. getBookingNumber();
                message = String.format(
                        "Dear %s, your booking %s has been confirmed by the technician. " +
                                "Scheduled for:  %s. The technician will arrive as per schedule.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber(),
                        event.getScheduledDate() != null ? event.getScheduledDate() : "As scheduled"
                );
                break;

            case BOOKING_REJECTED:
                notificationType = NotificationType.BOOKING_REJECTED;
                title = "Technician Unavailable - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, unfortunately the assigned technician is unavailable for booking %s. " +
                                "Reason: %s. We are assigning a new technician and will notify you shortly.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber(),
                        event. getRejectionReason() != null ? event.getRejectionReason() : "Schedule conflict"
                );
                break;

            case SERVICE_STARTED:
                notificationType = NotificationType.SERVICE_STARTED;
                title = "Service Started - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, the technician has started working on your booking %s. " +
                                "You will receive an OTP to verify service completion.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber()
                );
                break;

            case SERVICE_COMPLETED:
                notificationType = NotificationType.SERVICE_COMPLETED;
                title = "Service Completed - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, your service for booking %s has been completed successfully. " +
                                "Thank you for choosing us!  Please rate your experience.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber()
                );
                break;

            case BOOKING_CANCELLED:
                notificationType = NotificationType.BOOKING_CANCELLED;
                title = "Booking Cancelled - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, your booking %s has been cancelled. " +
                                "Reason: %s. If you have any questions, please contact support.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber(),
                        event.getCancellationReason() != null ? event.getCancellationReason() : "As requested"
                );
                break;

            case BOOKING_RESCHEDULED:
                notificationType = NotificationType. BOOKING_RESCHEDULED;
                title = "Booking Rescheduled - " + event.getBookingNumber();
                message = String.format(
                        "Dear %s, your booking %s has been rescheduled to %s. " +
                                "The technician will arrive as per the new schedule.",
                        getDisplayName(event.getUserName()),
                        event.getBookingNumber(),
                        event.getScheduledDate() != null ? event.getScheduledDate() : "the new date"
                );
                break;

            default:
                log.warn("Unhandled booking event type: {}", event.getEventType());
                return;
        }

        createAndSendNotification(
                event.getUserId(),
                event.getUserEmail(),
                event.getUserName(),
                event.getUserRole(),
                notificationType,
                title,
                message,
                event.getBookingId(),
                "BOOKING"
        );
    }

    private void handleBillingEvent(BillingEvent event) {
        log.info("Processing billing event: {} for invoice: {}",
                event.getEventType(), event.getInvoiceNumber());

        String title = "Invoice Generated - " + event. getInvoiceNumber();
        String message = String.format(
                "Dear %s, your invoice %s for ₹%. 2f has been generated for booking %s. " +
                        "Please make the payment at your earliest convenience.",
                getDisplayName(event. getUserName()),
                event.getInvoiceNumber(),
                event.getAmount() != null ? event.getAmount() : 0.0,
                event.getBookingNumber() != null ? event.getBookingNumber() : "your service"
        );

        createAndSendNotification(
                event.getUserId(),
                event. getUserEmail(),
                event.getUserName(),
                event.getUserRole(),
                NotificationType. INVOICE_GENERATED,
                title,
                message,
                event.getInvoiceId(),
                "INVOICE"
        );
    }

    private void createAndSendNotification(String userId, String userEmail, String userName,
                                           String userRole, NotificationType type, String title,
                                           String message, String referenceId, String referenceType) {

        Notification notification = Notification. builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .userRole(userRole)
                .type(type)
                .title(title)
                .message(message)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .status(NotificationStatus.PENDING)
                .emailSent(false)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: {} for user: {}", saved.getId(), userId);

        // Send email asynchronously
        if (userEmail != null && !userEmail.isEmpty()) {
            emailService.sendEmail(saved);
        } else {
            saved.setStatus(NotificationStatus.SENT);
            notificationRepository.save(saved);
        }
    }

    private String getDisplayName(String userName) {
        return userName != null && !userName.isEmpty() ? userName : "Customer";
    }
}