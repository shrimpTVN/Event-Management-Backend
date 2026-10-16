package com.ddd.application.dto.event;

import java.time.Instant;

public record EventSearchCriteriaDto(
        String keyword,
        String status,
        Long eventTypeId,
        Long semesterId,
        Long criteriaId,
        Long pointCategoryId,
        Instant dateHappenFrom,
        Instant dateHappenTo,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
) {
}
