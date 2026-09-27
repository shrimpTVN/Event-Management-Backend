package com.ddd.api.dto.eventtype.req;

import jakarta.validation.constraints.NotBlank;

public record CreateEventTypeRequestDto(
        @NotBlank String name,
        String description
) {
}
