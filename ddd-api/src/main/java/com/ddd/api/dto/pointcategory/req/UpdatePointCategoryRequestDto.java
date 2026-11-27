package com.ddd.api.dto.pointcategory.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record UpdatePointCategoryRequestDto(
        @NotBlank String name,
        String note,
        @PositiveOrZero Integer maximum,
        LocalDate dateApply,
        @Positive Long parentCategoryId,
        @Positive Integer childrenOrder,
        Boolean isActive,
        Boolean isMaxChild
) {
}
