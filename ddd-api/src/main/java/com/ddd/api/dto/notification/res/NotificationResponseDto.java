package com.ddd.api.dto.notification.res;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;

import java.time.Instant;

public record NotificationResponseDto(
        Long id,
        NotificationTypeEnum type,
        String message,
        NotificationStatusEnum status,
        String iconUrl,
        String url,
        Long fanpageId,
        Instant createdAt
) {
    public NotificationResponseDto(Long id, NotificationTypeEnum type, String message,
                                   NotificationStatusEnum status, String iconUrl, String url,
                                   Instant createdAt) {
        this(id, type, message, status, iconUrl, url, null, createdAt);
    }
}
