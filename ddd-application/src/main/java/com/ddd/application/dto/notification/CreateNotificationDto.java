package com.ddd.application.dto.notification;

import com.ddd.domain.enums.NotificationTypeEnum;

public record CreateNotificationDto(
        Long userId,
        Long fanpageId,
        NotificationTypeEnum type,
        String message,
        String iconUrl,
        String url
) {
    public static CreateNotificationDto forUser(Long userId, NotificationTypeEnum type, String message, String iconUrl, String url) {
        return new CreateNotificationDto(userId, null, type, message, iconUrl, url);
    }

    public static CreateNotificationDto forFanpage(Long fanpageId, NotificationTypeEnum type, String message, String iconUrl, String url) {
        return new CreateNotificationDto(null, fanpageId, type, message, iconUrl, url);
    }

    public CreateNotificationDto(Long userId, NotificationTypeEnum type, String message, String iconUrl, String url) {
        this(userId, null, type, message, iconUrl, url);
    }
}
