package com.ddd.api.dto.semesterpoint.res;

import java.time.Instant;

public record PointTransactionResponseDto(
        Long id,
        Long pointCategoryId,
        Long eventId,
        String eventName,
        Integer points,
        String type,
        String status,
        String description,
        Instant createdAt
) {
}
