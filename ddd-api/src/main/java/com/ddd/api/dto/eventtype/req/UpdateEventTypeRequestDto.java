package com.ddd.api.dto.eventtype.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEventTypeRequestDto(
        @NotBlank String name,
        String description,
        @NotNull Boolean isActive
) {
}
