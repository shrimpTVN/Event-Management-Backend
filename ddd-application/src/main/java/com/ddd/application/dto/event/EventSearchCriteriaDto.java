package com.ddd.application.dto.event;

import java.time.Instant;
import java.util.List;

public record EventSearchCriteriaDto(
        String keyword,
        String status,
        List<Long> eventTypeIds,
        Long semesterId,
        List<Long> criteriaIds,
        List<Long> pointCategoryIds,
        Instant dateHappenFrom,
        Instant dateHappenTo,
        Boolean isActive,
        Integer page,
        Integer size,
        String sortBy,
        String sortDir
) {
}
