package com.ddd.application.service.notification;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.notification.NotificationInfoDto;

public interface NotificationCommandService {
    NotificationInfoDto createNotification(CreateNotificationDto dto);

    void markAsRead(Long notificationId);

    void markAllAsRead();

    void deleteNotification(Long notificationId);
}
