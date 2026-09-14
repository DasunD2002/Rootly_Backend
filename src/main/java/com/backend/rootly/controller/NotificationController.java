package com.backend.rootly.controller;

import com.backend.rootly.dto.request.MarkNotificationReadDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.NotificationService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = {
            EndPoint.NOTIFICATIONS,
            "/notifications",
            "/v1/notifications"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getNotifications(
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "userId", required = false) String queryUserId,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String effectiveUserId = user != null ? user.getId() : queryUserId;
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (log.isDebugEnabled()) {
            log.debug("Received Get Notifications request for user {}", effectiveUserId);
        }
        return notificationService.getNotifications(effectiveUserId, locale);
    }

    @PatchMapping(value = {
            EndPoint.NOTIFICATIONS_MARK_READ,
            "/notifications/mark-read",
            "/v1/notifications/mark-read"
    }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> markAsRead(
            @AuthenticationPrincipal UserReg user,
            @RequestParam(value = "userId", required = false) String queryUserId,
            @RequestBody(required = false) MarkNotificationReadDTO requestDTO,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        String effectiveUserId = user != null ? user.getId() : queryUserId;
        if (effectiveUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String notificationId = requestDTO != null ? requestDTO.getNotificationId() : null;
        if (log.isDebugEnabled()) {
            log.debug("Received Mark Notifications Read request for user {}, notificationId {}",
                    effectiveUserId, notificationId);
        }
        return notificationService.markAsRead(effectiveUserId, notificationId, locale);
    }
}
