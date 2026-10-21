package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicYearJpaRepository extends JpaRepository<AcademicYearJpaEntity, Long> {
    List<AcademicYearJpaEntity> findAllByIsActiveTrueOrderByStartDateDesc();

    Optional<AcademicYearJpaEntity> findByCode(String code);

    Optional<AcademicYearJpaEntity> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(
            LocalDate date, LocalDate sameDate);
}
