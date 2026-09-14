package com.backend.rootly.service.impl;

import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.response.NotificationGroupDTO;
import com.backend.rootly.entity.Notification;
import com.backend.rootly.enums.NotificationType;
import com.backend.rootly.repository.NotificationRepository;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTests {

    private NotificationRepository notificationRepository;
    private NotificationServiceImpl notificationService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        notificationService = new NotificationServiceImpl(
                notificationRepository,
                responseGenerator,
                clock
        );
    }

    @Test
    void getNotificationsGroupsTodayAndYesterdayCorrectly() {
        Notification todayNotif = Notification.builder()
                .id("notif-1")
                .userId("user-1")
                .title("Memory Added")
                .message("New photo added")
                .type(NotificationType.MEMORY_ADDED)
                .isRead(false)
                .createdAt(Instant.parse("2026-09-14T08:00:00Z"))
                .build();

        Notification yesterdayNotif = Notification.builder()
                .id("notif-2")
                .userId("user-1")
                .title("Weekly Digest")
                .message("Review your week")
                .type(NotificationType.WEEKLY_DIGEST)
                .isRead(true)
                .createdAt(Instant.parse("2026-09-13T15:00:00Z"))
                .build();

        Notification earlierNotif = Notification.builder()
                .id("notif-3")
                .userId("user-1")
                .title("Security Check")
                .message("Login verified")
                .type(NotificationType.SECURITY_CHECK)
                .isRead(true)
                .createdAt(Instant.parse("2026-09-10T10:00:00Z"))
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(todayNotif, yesterdayNotif, earlierNotif));

        ResponseEntity<Object> response = notificationService.getNotifications("user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.NOTIFICATION_GET_SUCCESS);
        NotificationGroupDTO group = (NotificationGroupDTO) dto.getData();
        assertThat(group.getToday()).hasSize(1);
        assertThat(group.getToday().get(0).getTitle()).isEqualTo("Memory Added");
        assertThat(group.getYesterday()).hasSize(1);
        assertThat(group.getYesterday().get(0).getTitle()).isEqualTo("Weekly Digest");
        assertThat(group.getEarlier()).hasSize(1);
        assertThat(group.getUnreadCount()).isEqualTo(1L);
    }

    @Test
    void markSpecificNotificationAsReadSuccess() {
        Notification notif = Notification.builder()
                .id("notif-1")
                .userId("user-1")
                .isRead(false)
                .build();

        when(notificationRepository.findById("notif-1")).thenReturn(Optional.of(notif));

        ResponseEntity<Object> response = notificationService.markAsRead("user-1", "notif-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notif.getIsRead()).isTrue();
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAllNotificationsAsReadSuccess() {
        Notification notif1 = Notification.builder().id("1").userId("user-1").isRead(false).build();
        Notification notif2 = Notification.builder().id("2").userId("user-1").isRead(false).build();

        when(notificationRepository.findByUserIdAndIsReadFalse("user-1")).thenReturn(List.of(notif1, notif2));

        ResponseEntity<Object> response = notificationService.markAsRead("user-1", null, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(notif1.getIsRead()).isTrue();
        assertThat(notif2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(any());
    }
}
