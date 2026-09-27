package com.ddd.api.dto.eventtype.res;

public record EventTypeResponseDto(
        Long id,
        String name,
        String description,
        boolean isActive
) {
}
