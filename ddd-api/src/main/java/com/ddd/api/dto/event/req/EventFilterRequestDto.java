package com.ddd.api.dto.event.req;

import com.ddd.domain.enums.EventStatusEnum;

import java.time.Instant;
import java.util.List;

public record EventFilterRequestDto(
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
    }

    public EventFilterRequestDto(EventFilterRequestDto other) {
        this(
                other.keyword,
                (other.status == null || other.status.isBlank())
                        && (other.statuses == null || other.statuses.isEmpty())
                        ? EventStatusEnum.PUBLISHED.name()
                        : other.status,
                other.statuses,
                other.eventTypeIds,
                other.semesterId,
                other.schoolId,
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
