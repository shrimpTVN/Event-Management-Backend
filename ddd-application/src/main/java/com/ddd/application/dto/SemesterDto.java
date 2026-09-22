package com.ddd.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

public record SemesterDto(@Positive Integer number,
                          @NotNull String academicYear,
                          @NotNull LocalDate startDate,
                          @NotNull LocalDate endDate,
                          @NotNull String status,
                          @Positive Long id) implements Serializable {
}