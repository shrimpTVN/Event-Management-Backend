package com.ddd.api.dto.pointcategory.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record UpdatePointCategoryRequestDto(
        @NotBlank String name,
        String note,
        @Positive Integer maximum,
        LocalDate dateApply,
        @NotNull @Positive Long parentCategoryId,
        @Positive Integer childrenOrder,
        Boolean isActive
) {
}
