package com.ddd.api.dto.semester;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record SemesterRequestDto(
        @Positive @Max(3) int number,
        String academicYear,
        @Positive Long academicYearId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
