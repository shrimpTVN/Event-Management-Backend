package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EventPointRequestDto(
        @NotNull @Positive Long pointCategoryId,
        @NotNull @Positive Long point
) {
}
