package com.ddd.api.dto.criteria.res;

import java.time.Instant;

public record CriteriaTransactionResponseDto(
        Long id,
        Long userId,
        Long criteriaId,
        Long academicYearId,
        Long eventId,
        String eventName,
        String completionSource,
        String note,
        Boolean isActive,
        Instant createdAt
) {
}
