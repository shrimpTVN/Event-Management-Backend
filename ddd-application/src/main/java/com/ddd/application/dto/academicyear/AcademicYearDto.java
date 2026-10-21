package com.ddd.application.dto.academicyear;

import java.time.LocalDate;

public record AcademicYearDto(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive
) {
}
