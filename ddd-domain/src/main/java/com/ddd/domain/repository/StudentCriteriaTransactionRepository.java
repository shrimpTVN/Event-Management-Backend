package com.ddd.domain.repository;

import com.ddd.domain.model.StudentCriteriaTransaction;

import java.util.List;

public interface StudentCriteriaTransactionRepository {
    void save(StudentCriteriaTransaction transaction);

    void saveAll(List<StudentCriteriaTransaction> toSave);
}
