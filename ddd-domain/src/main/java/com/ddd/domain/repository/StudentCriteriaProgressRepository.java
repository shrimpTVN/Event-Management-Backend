package com.ddd.domain.repository;

import com.ddd.domain.model.StudentCriteriaProgress;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentCriteriaProgressRepository {
    List<StudentCriteriaProgress> findByUserIdAndAcademicYearId(Long userId, Long academicYearId);

    Optional<StudentCriteriaProgress> findByUserIdAndCriteriaIdAndAcademicYearId(
            Long userId, Long criteriaId, Long academicYearId);

    List<StudentCriteriaProgress> findByUserIdsAndCriteriaIdAndAcademicYearId(
            Collection<Long> userIds, Long criteriaId, Long academicYearId);

    StudentCriteriaProgress save(StudentCriteriaProgress progress);

    List<StudentCriteriaProgress> saveAll(List<StudentCriteriaProgress> progresses);

    boolean existByUserIdAndCriteriaIdAndAcademicYearId(Long userId, Long criteriaId, Long academicYearId);

    List<StudentCriteriaProgress> findAllByUserIdAndAcademicYearId(Long userId, Long academicYearId);

    List<StudentCriteriaProgress> findAllByUserIdsAndAcademicYearId(Collection<Long> userIds, Long academicYearId);
}
