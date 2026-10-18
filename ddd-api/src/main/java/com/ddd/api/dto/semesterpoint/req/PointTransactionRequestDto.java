package com.ddd.api.dto.semesterpoint.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointTransactionRequestDto(
        @Positive Long userId,
        @NotNull @Positive Long semesterId,
        @NotNull @Positive Long pointCategoryId,
        @Positive Long eventId,
        @NotNull @Positive Integer points,
        String description
) {
}
