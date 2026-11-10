package com.ddd.application.dto.notification;

public record CreateNotificationDto(
        Long userId,
        Long fanpageId,
        String title,
        String message,
        String iconUrl,
        String url
) {
    public static CreateNotificationDto forUser(Long userId, String title, String message, String iconUrl, String url) {
        return new CreateNotificationDto(userId, null, title, message, iconUrl, url);
    }

    public static CreateNotificationDto forFanpage(Long fanpageId, String title, String message, String iconUrl, String url) {
        return new CreateNotificationDto(null, fanpageId, title, message, iconUrl, url);
    }

    public CreateNotificationDto(Long userId, String title, String message, String iconUrl, String url) {
        this(userId, null, title, message, iconUrl, url);
    }
}
