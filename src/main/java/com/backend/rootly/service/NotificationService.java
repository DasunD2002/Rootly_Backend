package com.backend.rootly.service;

import com.backend.rootly.enums.NotificationType;
import org.springframework.http.ResponseEntity;

import java.util.Locale;

public interface NotificationService {

    ResponseEntity<Object> getNotifications(String userId, Locale locale);

    ResponseEntity<Object> markAsRead(String userId, String notificationId, Locale locale);

    void createNotification(String userId, String title, String message, NotificationType type);
}
