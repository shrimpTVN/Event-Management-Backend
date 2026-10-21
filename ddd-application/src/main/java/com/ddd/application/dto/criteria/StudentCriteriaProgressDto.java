package com.ddd.application.dto.criteria;

import java.util.List;

public record StudentCriteriaProgressDto(
        Long academicYearId,
        String academicYear,
        int completedMainCriteria,
        int totalMainCriteria,
        List<CriteriaProgressItemDto> criterias,
        List<LeafProgressDetailDto> progress
) {
}
