package com.ddd.api.dto.academicyear.res;

import java.io.Serializable;
import java.time.LocalDate;

public record AcademicYearResponseDto(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive
) implements Serializable {
}
