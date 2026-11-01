package com.ddd.application.service.criteriaprogress;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public interface StudentCriteriaTransactionCommandService {
    void createTransaction(String email, Long criteriaId, Long academicYearId, String note);

    void createTransactionFromEvent(Long userId, Long eventId);

    void createTransactionFromEvent(List<Long> studentUserIds, Long eventId);
}
