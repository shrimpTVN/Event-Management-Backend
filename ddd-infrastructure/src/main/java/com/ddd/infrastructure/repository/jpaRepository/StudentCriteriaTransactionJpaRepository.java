package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentCriteriaTransactionJpaRepository
        extends JpaRepository<StudentCriteriaTransactionJpaEntity, Long> {

    @EntityGraph(attributePaths = {"event"})
    List<StudentCriteriaTransactionJpaEntity> findByUserIdAndCriteriaIdAndAcademicYearId(Long id, Long criteriaId, Long academicYearId);
}
