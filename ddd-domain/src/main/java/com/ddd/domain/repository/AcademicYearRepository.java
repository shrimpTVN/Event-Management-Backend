package com.ddd.domain.repository;

import com.ddd.domain.model.AcademicYear;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcademicYearRepository {
    List<AcademicYear> findAllActive();

    Optional<AcademicYear> findById(Long id);

    Optional<AcademicYear> findByCode(String code);

    Optional<AcademicYear> findCurrent(LocalDate date);

    AcademicYear save(AcademicYear academicYear);
}
