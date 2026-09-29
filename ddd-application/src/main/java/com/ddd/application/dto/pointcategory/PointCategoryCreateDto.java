package com.ddd.application.dto.pointcategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PointCategoryCreateDto(
                @NotNull @NotBlank String name,
                @NotNull @NotBlank String description,
                @NotNull @Positive Integer maximum,
                @NotNull LocalDate dateApply,
                @Positive Long parentCategoryId) {
}
