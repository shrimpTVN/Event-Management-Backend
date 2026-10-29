package com.ddd.application.service.criteriaprogress;

import java.util.List;

public interface StudentCriteriaProgressCommandService {
    void markCompleted(Long userId, Long criteriaId, Long academicYearId);

    void bulkMarkCompleted(List<Long> userIds, Long criteriaId, Long academicYearId);
}
