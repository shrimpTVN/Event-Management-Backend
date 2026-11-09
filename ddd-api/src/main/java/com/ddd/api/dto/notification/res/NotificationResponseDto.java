package com.ddd.api.dto.notification.res;

import com.ddd.domain.enums.NotificationStatusEnum;

import java.time.Instant;

public record NotificationResponseDto(
        Long id,
        String title,
        String message,
        NotificationStatusEnum status,
        String iconUrl,
        String url,
        Instant createdAt
) {
}
