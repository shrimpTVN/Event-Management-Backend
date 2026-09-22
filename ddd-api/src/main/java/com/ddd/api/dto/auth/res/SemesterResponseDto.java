package com.ddd.api.dto.auth.res;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

public record SemesterResponseDto(@Positive Integer number,
                                  @NotNull String academicYear,
                                  @NotNull LocalDate startDate,
                                  @NotNull LocalDate endDate,
                                  @NotNull String status,
                                  @NotNull Boolean isCurrent,
                                  @Positive Long id) implements Serializable {
}