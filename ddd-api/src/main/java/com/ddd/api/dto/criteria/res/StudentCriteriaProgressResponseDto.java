package com.ddd.api.dto.criteria.res;

import java.util.List;

public record StudentCriteriaProgressResponseDto(
        Long academicYearId,
        String academicYear,
        int completedMainCriteria,
        int totalMainCriteria,
        List<CriteriaProgressItemResponseDto> criterias
) {
}
