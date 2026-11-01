package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCriteriaProgressJpaRepository
        extends JpaRepository<StudentCriteriaProgressJpaEntity, Long> {

    @EntityGraph(attributePaths = {"criteria"})
    List<StudentCriteriaProgressJpaEntity> findAllByUserIdAndAcademicYearId(Long userId, Long academicYearId);

    Optional<StudentCriteriaProgressJpaEntity> findByUserIdAndCriteriaIdAndAcademicYearId(
            Long userId, Long criteriaId, Long academicYearId);

    List<StudentCriteriaProgressJpaEntity> findAllByUserIdInAndCriteriaIdAndAcademicYearId(
            Collection<Long> userIds, Long criteriaId, Long academicYearId);

    List<StudentCriteriaProgressJpaEntity> findAllByUserIdInAndAcademicYearId(
            Collection<Long> userIds, Long academicYearId);

    boolean existsByUserIdAndCriteriaIdAndAcademicYearId(Long userId, Long criteriaId, Long academicYearId);
}
