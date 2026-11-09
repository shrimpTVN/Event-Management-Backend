package com.ddd.application.service.notification.impl;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.mapper.NotificationDtoMapper;
import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPushPort notificationPushPort;

    @Mock
    private NotificationDtoMapper notificationDtoMapper;

    @Mock
    private SecurityContextPort securityContextPort;

    @InjectMocks
    private NotificationCommandServiceImpl notificationCommandService;

    @Test
    @DisplayName("Should create notification, push via SSE, and return DTO")
    void createNotification_success() {
        CreateNotificationDto dto = new CreateNotificationDto(
                1L, "Event Approved", "Your event was approved", "icon.png", "/events/1"
        );

        Notification saved = new Notification("Event Approved", "Your event was approved",
                NotificationStatusEnum.UNREAD, "icon.png", "/events/1", 1L);
        saved.setId(10L);

        NotificationInfoDto expectedDto = new NotificationInfoDto(
                10L, "Event Approved", "Your event was approved",
                NotificationStatusEnum.UNREAD, "icon.png", "/events/1", 1L, Instant.now()
        );

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);
        when(notificationDtoMapper.toInfoDto(saved)).thenReturn(expectedDto);

        NotificationInfoDto result = notificationCommandService.createNotification(dto);

        assertNotNull(result);
        assertEquals(10L, result.id());
        verify(notificationRepository).save(any(Notification.class));
        verify(notificationPushPort).pushToUser(eq(1L), eq(saved));
    }

    @Test
    @DisplayName("Should mark notification as read when user owns the notification")
    void markAsRead_success() {
        Long notificationId = 10L;
        Long userId = 1L;

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertDoesNotThrow(() -> notificationCommandService.markAsRead(notificationId));

        verify(notificationRepository).markAsRead(notificationId, userId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when notification belongs to different user")
    void markAsRead_whenNotOwner_shouldThrow() {
        Long notificationId = 10L;
        Long currentUserId = 1L;
        Long otherUserId = 2L;

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(otherUserId);

        when(securityContextPort.getCurrentUserId()).thenReturn(currentUserId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(ResourceNotFoundException.class, () -> notificationCommandService.markAsRead(notificationId));
        verify(notificationRepository, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when user is not authenticated")
    void markAsRead_whenUnauthenticated_shouldThrow() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> notificationCommandService.markAsRead(10L));
    }

    @Test
    @DisplayName("Should mark all notifications as read for current user")
    void markAllAsRead_success() {
        Long userId = 1L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        notificationCommandService.markAllAsRead();

        verify(notificationRepository).markAllAsRead(userId);
    }

    @Test
    @DisplayName("Should delete notification when user owns it")
    void deleteNotification_success() {
        Long notificationId = 10L;
        Long userId = 1L;

        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertDoesNotThrow(() -> notificationCommandService.deleteNotification(notificationId));

        verify(notificationRepository).deleteByIdAndUserId(notificationId, userId);
    }
}
