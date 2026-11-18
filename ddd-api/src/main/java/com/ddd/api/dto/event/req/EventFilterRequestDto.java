package com.ddd.api.dto.event.req;

import com.ddd.domain.enums.EventStatusEnum;

import java.time.Instant;
import java.util.List;

public record EventFilterRequestDto(
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
    public EventFilterRequestDto {

        if (page == null) {
            page = 0;
        }
        if (size == null) {
            size = 10;
        }
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        if (sortDir == null || sortDir.isBlank()) {
            sortDir = "desc";
        }
        if (isActive == null) {
            isActive = true;
        }
        if (status == null || status.isBlank()) {
            status = EventStatusEnum.PUBLISHED.name();
        }
    }

    public EventFilterRequestDto(EventFilterRequestDto other) {
        this(
                other.keyword,
                other.status,
                other.eventTypeIds,
                other.semesterId,
                other.criteriaIds,
                other.pointCategoryIds,
                other.dateHappenFrom,
                other.dateHappenTo,
                true,
                other.page,
                other.size,
                other.sortBy,
                other.sortDir
        );
    }
}
