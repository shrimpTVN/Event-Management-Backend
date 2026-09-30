package com.ddd.application.dto.event;

public record EventPointCreateDto(
        Long pointCategoryId,
        Long point
) {
}
