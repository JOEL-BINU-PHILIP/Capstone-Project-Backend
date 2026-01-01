package com.app.notification.service.impl;

import com.app.notification.dto.request.SendNotificationRequest;
import com. app.notification.dto.response. NotificationResponse;
import com. app.notification.exception.NotificationException;
import com.app.notification.exception.ResourceNotFoundException;
import com. app.notification.model.Notification;
import com.app.notification.model.NotificationStatus;
import com.app.notification.model.NotificationType;
import com.app.notification.repository.NotificationRepository;
import com.app.notification.service.EmailService;
import com.app.notification. service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time. Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .userName(request. getUserName())
                .userRole(request.getUserRole())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .referenceId(request. getReferenceId())
                .referenceType(request.getReferenceType())
                .status(NotificationStatus.PENDING)
                .emailSent(false)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created:  {} for user: {}", saved.getId(), request.getUserId());

        // Send email asynchronously if requested
        if (request.isSendEmail() && request.getUserEmail() != null) {
            emailService.sendEmail(saved);
        } else {
            saved.setStatus(NotificationStatus.SENT);
            notificationRepository.save(saved);
        }

        return toResponse(saved);
    }

    // ==================== QUICK SEND METHODS ====================

    @Override
    public NotificationResponse sendBookingCreatedNotification(
            String userId, String userEmail, String userName, String bookingId, String bookingNumber) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .type(NotificationType.BOOKING_CREATED)
                .title("Booking Confirmed - " + bookingNumber)
                .message("Your service booking " + bookingNumber + " has been successfully created.  " +
                        "We will assign a technician shortly and notify you.")
                .referenceId(bookingId)
                .referenceType("BOOKING")
                .sendEmail(true)
                .build();

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendTechnicianAssignedNotification(
            String userId, String userEmail, String userName, String bookingId, String technicianName) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .type(NotificationType.TECHNICIAN_ASSIGNED)
                .title("Technician Assigned to Your Booking")
                .message("Good news! " + technicianName + " has been assigned to your service request. " +
                        "The technician will contact you shortly to confirm the schedule.")
                .referenceId(bookingId)
                .referenceType("BOOKING")
                .sendEmail(true)
                .build();

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendServiceCompletedNotification(
            String userId, String userEmail, String userName, String bookingId) {

        SendNotificationRequest request = SendNotificationRequest. builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .type(NotificationType.SERVICE_COMPLETED)
                .title("Service Completed Successfully")
                .message("Your service has been completed successfully. " +
                        "Thank you for choosing our services.  Please rate your experience.")
                .referenceId(bookingId)
                .referenceType("BOOKING")
                .sendEmail(true)
                .build();

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendInvoiceGeneratedNotification(
            String userId, String userEmail, String userName, String invoiceId, String invoiceNumber, Double amount) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .type(NotificationType.INVOICE_GENERATED)
                .title("Invoice Generated - " + invoiceNumber)
                .message("Your invoice " + invoiceNumber + " for ₹" + String.format("%.2f", amount) +
                        " has been generated.  Please make the payment at your earliest convenience.")
                .referenceId(invoiceId)
                .referenceType("INVOICE")
                .sendEmail(true)
                .build();

        return sendNotification(request);
    }

    @Override
    public NotificationResponse sendPaymentReceivedNotification(
            String userId, String userEmail, String userName, String invoiceNumber, Double amount) {

        SendNotificationRequest request = SendNotificationRequest.builder()
                .userId(userId)
                .userEmail(userEmail)
                .userName(userName)
                .type(NotificationType. PAYMENT_RECEIVED)
                .title("Payment Received - Thank You!")
                .message("We have received your payment of ₹" + String.format("%.2f", amount) +
                        " for invoice " + invoiceNumber + ". Thank you for your payment!")
                .referenceId(invoiceNumber)
                .referenceType("INVOICE")
                .sendEmail(true)
                .build();

        return sendNotification(request);
    }

    // ==================== READ ====================

    @Override
    public NotificationResponse getNotificationById(String notificationId) {
        return toResponse(getNotificationEntity(notificationId));
    }

    // ==================== LIST ====================

    @Override
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationResponse> getUserNotificationsPaged(String userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository. countByUserIdAndIsReadFalse(userId);
    }

    // ==================== MARK AS READ ====================

    @Override
    @Transactional
    public NotificationResponse markAsRead(String notificationId, String userId) {
        Notification notification = getNotificationEntity(notificationId);

        // Verify ownership
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationException("You don't have access to this notification");
        }

        if (! notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
            log.info("Notification {} marked as read by user {}", notificationId, userId);
        }

        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);

        Instant now = Instant.now();
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(now);
            notification.setStatus(NotificationStatus. READ);
        }

        notificationRepository.saveAll(unreadNotifications);
        log.info("Marked {} notifications as read for user {}", unreadNotifications. size(), userId);
    }

    // ==================== DELETE ====================

    @Override
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = getNotificationEntity(notificationId);

        // Verify ownership
        if (!notification.getUserId().equals(userId)) {
            throw new NotificationException("You don't have access to this notification");
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, userId);
    }

    // ==================== ADMIN ====================

    @Override
    public List<NotificationResponse> getNotificationsByType(NotificationType type) {
        return notificationRepository.findByType(type)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<NotificationResponse> getAllNotificationsPaged(Pageable pageable) {
        return notificationRepository.findAll(pageable)
                .map(this::toResponse);
    }

    // ==================== HELPER METHODS ====================

    private Notification getNotificationEntity(String notificationId) {
        return notificationRepository. findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse. builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .userEmail(notification.getUserEmail())
                .userName(notification.getUserName())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification. getReferenceType())
                .status(notification.getStatus())
                .emailSent(notification. isEmailSent())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}