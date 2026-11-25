package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterJpaRepository extends JpaRepository<SemesterJpaEntity, Long> {
    List<SemesterJpaEntity> findAllByIsActive(boolean isActive);

    boolean existsByNumberAndStartDateAndEndDate(Integer number, LocalDate startDate, LocalDate endDate);

    boolean existsByStartDateBeforeAndEndDateAfter(LocalDate startDate, LocalDate endDate);

    boolean existsByAcademicYearIdAndNumberAndIsActiveTrue(Long academicYearId, Integer number);

    boolean existsByAcademicYearIdAndNumberAndIsActiveTrueAndIdNot(Long academicYearId, Integer number, Long id);

    @Query("""
            SELECT COUNT(s) > 0
            FROM SemesterJpaEntity s
            WHERE s.isActive = true
              AND s.startDate <= :endDate
              AND s.endDate >= :startDate
              AND (:excludeId IS NULL OR s.id <> :excludeId)
            """)
    boolean existsActiveOverlappingDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") Long excludeId
    );

    Optional<SemesterJpaEntity> findByStatus(SemesterStatusEnum status);
}
