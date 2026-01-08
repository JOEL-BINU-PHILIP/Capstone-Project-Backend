package com.app.notification.service. impl;

import com.app. notification.model.Notification;
import com. app.notification.model.NotificationStatus;
import com.app. notification.repository.NotificationRepository;
import com.app.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.beans.factory.annotation. Value;
import org.springframework. mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org. springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    @Override
    @Async("notificationExecutor")
    public void sendEmail(Notification notification) {
        if (! emailEnabled) {
            log.info("Email sending is disabled.  Skipping email for notification: {}", notification.getId());
            updateNotificationEmailStatus(notification, true, null);
            return;
        }

        if (notification.getUserEmail() == null || notification.getUserEmail().isEmpty()) {
            log.warn("No email address for notification: {}", notification.getId());
            updateNotificationEmailStatus(notification, false, "No email address provided");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notification.getUserEmail());
            message.setSubject(notification.getTitle());
            message.setText(buildEmailBody(notification));

            mailSender.send(message);

            log.info("Email sent successfully to: {} for notification: {}",
                    notification.getUserEmail(), notification.getId());

            updateNotificationEmailStatus(notification, true, null);

        } catch (Exception e) {
            log.error("Failed to send email to: {} for notification: {}.  Error: {}",
                    notification.getUserEmail(), notification.getId(), e.getMessage());

            updateNotificationEmailStatus(notification, false, e.getMessage());
        }
    }

    @Override
    @Async("notificationExecutor")
    public void sendSimpleEmail(String to, String subject, String body) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping email to: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message. setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send simple email to: {}. Error: {}", to, e.getMessage());
        }
    }

    private String buildEmailBody(Notification notification) {
        StringBuilder body = new StringBuilder();

        body.append("Dear ").append(notification.getUserName() != null ? notification.getUserName() : "Customer").append(",\n\n");
        body.append(notification.getMessage()).append("\n\n");

        if (notification.getReferenceId() != null) {
            body.append("Reference: ").append(notification.getReferenceType())
                    .append(" #").append(notification.getReferenceId()).append("\n\n");
        }

        body.append("Thank you,\n");
        body.append("Service Management Team");

        return body.toString();
    }

    private void updateNotificationEmailStatus(Notification notification, boolean sent, String error) {
        notification.setEmailSent(sent);
        notification.setEmailSentAt(Instant.now());
        notification.setEmailError(error);

        if (sent) {
            notification.setStatus(NotificationStatus. SENT);
        } else {
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }
}