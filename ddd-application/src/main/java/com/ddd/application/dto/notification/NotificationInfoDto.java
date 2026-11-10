package com.ddd.application.dto.notification;

import com.ddd.domain.enums.NotificationStatusEnum;

import java.time.Instant;

public record NotificationInfoDto(
        Long id,
        String title,
        String message,
        NotificationStatusEnum status,
        String iconUrl,
        String url,
        Long userId,
        Long fanpageId,
        Instant createdAt
) {
    public NotificationInfoDto(Long id, String title, String message, NotificationStatusEnum status, String iconUrl, String url, Long userId, Instant createdAt) {
        this(id, title, message, status, iconUrl, url, userId, null, createdAt);
    }
}
