package com.ddd.application.dto;

import com.ddd.domain.enums.SemesterStatusEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.time.LocalDate;

public record SemesterDto(@Positive Integer number,
        @NotNull String academicYear,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        SemesterStatusEnum status,
        @Positive Long id) implements Serializable {
}