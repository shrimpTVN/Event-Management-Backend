package com.ddd.application.dto.notification;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;

import java.time.Instant;

public record NotificationInfoDto(
        Long id,
        NotificationTypeEnum type,
        String message,
        NotificationStatusEnum status,
        String iconUrl,
        String url,
        Long userId,
        Long fanpageId,
        Instant createdAt
) {
    public NotificationInfoDto(Long id, NotificationTypeEnum type, String message, NotificationStatusEnum status, String iconUrl, String url, Long userId, Instant createdAt) {
        this(id, type, message, status, iconUrl, url, userId, null, createdAt);
    }
}
