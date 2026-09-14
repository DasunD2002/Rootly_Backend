package com.backend.rootly.service.impl;

import com.backend.rootly.dto.response.NotificationGroupDTO;
import com.backend.rootly.dto.response.NotificationResponseDTO;
import com.backend.rootly.entity.Notification;
import com.backend.rootly.enums.NotificationType;
import com.backend.rootly.repository.NotificationRepository;
import com.backend.rootly.service.NotificationService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Log4j2
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   ResponseGenerator responseGenerator,
                                   @Autowired(required = false) Clock clock) {
        this.notificationRepository = notificationRepository;
        this.responseGenerator = responseGenerator;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public ResponseEntity<Object> getNotifications(String userId, Locale locale) {
        List<Notification> allNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        LocalDate todayDate = LocalDate.now(clock);
        Instant startOfToday = todayDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfYesterday = todayDate.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        List<NotificationResponseDTO> today = new ArrayList<>();
        List<NotificationResponseDTO> yesterday = new ArrayList<>();
        List<NotificationResponseDTO> earlier = new ArrayList<>();
        long unreadCount = 0;

        for (Notification n : allNotifications) {
            if (Boolean.FALSE.equals(n.getIsRead())) {
                unreadCount++;
            }
            NotificationResponseDTO dto = NotificationResponseDTO.builder()
                    .id(n.getId())
                    .userId(n.getUserId())
                    .title(n.getTitle())
                    .message(n.getMessage())
                    .type(n.getType())
                    .isRead(n.getIsRead())
                    .createdAt(n.getCreatedAt())
                    .build();

            Instant itemTime = n.getCreatedAt() != null ? n.getCreatedAt() : Instant.now(clock);
            if (!itemTime.isBefore(startOfToday)) {
                today.add(dto);
            } else if (!itemTime.isBefore(startOfYesterday)) {
                yesterday.add(dto);
            } else {
                earlier.add(dto);
            }
        }

        NotificationGroupDTO groupDTO = NotificationGroupDTO.builder()
                .today(today)
                .yesterday(yesterday)
                .earlier(earlier)
                .unreadCount(unreadCount)
                .build();

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.NOTIFICATION_GET_SUCCESS, MessageConstant.NOTIFICATION_GET_SUCCESS, groupDTO);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> markAsRead(String userId, String notificationId, Locale locale) {
        if (notificationId != null && !notificationId.isBlank()) {
            Notification notification = notificationRepository.findById(notificationId.trim()).orElse(null);
            if (notification != null && userId.equals(notification.getUserId())) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
        } else {
            List<Notification> unreadList = notificationRepository.findByUserIdAndIsReadFalse(userId);
            for (Notification n : unreadList) {
                n.setIsRead(true);
            }
            notificationRepository.saveAll(unreadList);
        }

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.NOTIFICATION_UPDATE_SUCCESS, MessageConstant.NOTIFICATION_UPDATE_SUCCESS, null);
    }

    @Override
    public void createNotification(String userId, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(Instant.now(clock))
                .build();
        notificationRepository.save(notification);
        if (log.isDebugEnabled()) {
            log.debug("Created notification for user {}: {}", userId, title);
        }
    }
}
