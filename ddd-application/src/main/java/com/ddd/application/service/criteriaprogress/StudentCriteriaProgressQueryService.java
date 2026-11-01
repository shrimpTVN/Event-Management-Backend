package com.ddd.application.service.criteriaprogress;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;

public interface StudentCriteriaProgressQueryService {
    StudentCriteriaProgressDto getProgress(String email, Long academicYearId);

    CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId);
}
