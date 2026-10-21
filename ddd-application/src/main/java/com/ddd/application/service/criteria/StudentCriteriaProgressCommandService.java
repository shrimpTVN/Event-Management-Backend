package com.ddd.application.service.criteria;

import java.util.List;

public interface StudentCriteriaProgressCommandService {
    void setManualProgress(String email, Long criteriaId, Long academicYearId, String note);

    void markCompletedFromEvent(Long userId, Long eventId);

    void markCompletedFromEvent(List<Long> userIds, Long eventId);
}
