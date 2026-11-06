package com.ddd.domain.repository;

import com.ddd.domain.model.StudentCriteriaTransaction;
import java.util.List;
import java.util.Optional;

public interface StudentCriteriaTransactionRepository {
    void save(StudentCriteriaTransaction transaction);

    void saveAll(List<StudentCriteriaTransaction> toSave);

    boolean existsByIdAndUserIdAndCompletionSource(Long criteriaTransactionId, Long userId, String name);

    Optional<StudentCriteriaTransaction> findById(Long criteriaTransactionId);

    void delete(StudentCriteriaTransaction transaction);
}
