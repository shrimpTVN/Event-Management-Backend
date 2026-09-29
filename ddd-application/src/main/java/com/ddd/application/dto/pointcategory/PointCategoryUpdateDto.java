package com.ddd.application.dto.pointcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PointCategoryUpdateDto(
        @NotNull @NotBlank String name,
        String note,
        @NotNull @Positive Integer maximum,
        @NotNull LocalDate dateApply,
        @Positive Long parentCategoryId,
        @NotNull @Positive Integer childrenOrder,
        Boolean isActive
) {
}
