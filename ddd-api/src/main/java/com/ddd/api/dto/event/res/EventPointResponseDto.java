package com.ddd.api.dto.event.res;

public record EventPointResponseDto(
        Long pointCategoryId,
        Long eventId,
        Long point
) {
}