package com.ddd.application.dto.notification;

public record CreateNotificationDto(
        Long userId,
        String title,
        String message,
        String iconUrl,
        String url
) {
}
