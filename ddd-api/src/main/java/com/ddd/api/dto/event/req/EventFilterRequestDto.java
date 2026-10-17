package com.ddd.api.dto.event.req;

import com.ddd.domain.enums.EventStatusEnum;

import java.time.Instant;

public record EventFilterRequestDto(
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
                other.eventTypeId,
                other.semesterId,
                other.criteriaId,
                other.pointCategoryId,
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
