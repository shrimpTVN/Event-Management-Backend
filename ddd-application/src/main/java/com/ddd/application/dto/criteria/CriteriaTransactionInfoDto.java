package com.ddd.application.dto.criteria;

import java.time.Instant;

public record CriteriaTransactionInfoDto(
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
