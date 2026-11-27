package com.ddd.application.dto.pointcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record PointCategoryUpdateDto(
        @NotNull @NotBlank String name,
        String note,
        @NotNull @PositiveOrZero Integer maximum,
        @NotNull LocalDate dateApply,
        @Positive Long parentCategoryId,
        @NotNull @Positive Integer childrenOrder,
        Boolean isActive,
        Boolean isMaxChild
) {
}
