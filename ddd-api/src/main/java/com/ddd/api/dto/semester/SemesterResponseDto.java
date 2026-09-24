package com.ddd.api.dto.semester;

import com.ddd.domain.enums.SemesterStatusEnum;

import java.io.Serializable;
import java.time.LocalDate;

public record SemesterResponseDto(Integer number,
        String academicYear,
        LocalDate startDate,
        LocalDate endDate,
        SemesterStatusEnum status,
        Long id) implements Serializable {
}