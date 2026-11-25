package com.ddd.application.dto.event;

import com.ddd.domain.enums.EventStatusEnum;

import java.time.Instant;
import java.util.List;

public record EventSearchCriteriaDto(
        String keyword,
        String status,
        List<EventStatusEnum> statuses,
        List<Long> eventTypeIds,
        Long semesterId,
        Long schoolId,
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
    public EventSearchCriteriaDto(
            String keyword,
            String status,
            List<Long> eventTypeIds,
            Long semesterId,
            Long schoolId,
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
        this(keyword, status, null, eventTypeIds, semesterId, schoolId, criteriaIds, pointCategoryIds,
                dateHappenFrom, dateHappenTo, isActive, page, size, sortBy, sortDir);
    }
}
