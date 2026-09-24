package com.ddd.domain.repository;

import com.ddd.domain.model.Semester;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SemesterRepository {
    List<Semester> findAll();

    Semester save(Semester semester);

    Optional<Semester> findById(Long id);

    Semester getCurrent();

    void delete(Long id);

    boolean existsByNumberAndDateRange(Integer number, LocalDate startDate, LocalDate endDate);

    boolean existsOverlappingDateRange(LocalDate startDate, LocalDate endDate);

    List<Semester> findAllActive();
}
