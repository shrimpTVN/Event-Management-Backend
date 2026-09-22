package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.SemesterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterJpaRepository extends JpaRepository<SemesterJpaEntity, Long> {
    List<SemesterJpaEntity> findAllByIsActive(boolean isActive);

    boolean existsByNumberAndStartDateAndEndDate(Integer number, LocalDate startDate, LocalDate endDate);

    boolean existsByStartDateBeforeAndEndDateAfter(LocalDate startDate, LocalDate endDate);

    Optional<SemesterJpaEntity> findByStatus(String name);
}
