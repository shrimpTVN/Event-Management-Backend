package com.ddd.application.service.criteriaprogress;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.domain.enums.CriteriaScopeEnum;

public interface StudentCriteriaProgressQueryService {
    StudentCriteriaProgressDto getProgress(String email, Long academicYearId);

    StudentCriteriaProgressDto getProgress(String email, Long academicYearId, CriteriaScopeEnum scope);

    CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId);

    CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId, CriteriaScopeEnum scope);
}
