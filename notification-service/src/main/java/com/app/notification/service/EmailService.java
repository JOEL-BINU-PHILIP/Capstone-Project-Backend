package com.app.notification.service;

import com.app.notification.model.Notification;

public interface EmailService {

    /**
     * Send email notification asynchronously
     */
    void sendEmail(Notification notification);

    /**
     * Send a simple email
     */
    void sendSimpleEmail(String to, String subject, String body);
}