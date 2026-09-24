package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record SemesterRequestDto(
        @Positive @Max(3) int number,
        @NotNull @NotBlank String academicYear,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
