package com.ddd.application.dto.event;

public record EventPointInfoDto(
        Long pointCategoryId,
        Long eventId,
        Integer point
) {
}
