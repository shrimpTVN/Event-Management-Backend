package com.ddd.domain.repository;

import com.ddd.domain.model.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SchoolRepository {
    List<School> findAll();

    void deleteSchool(Long id);

    Optional<School> findById(Long id);

    School updateSchool(Long id, School school);

    School save(School school);
}
