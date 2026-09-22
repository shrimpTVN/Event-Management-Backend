package com.ddd.domain.repository;

import com.ddd.domain.model.Semester;

import java.util.List;

public interface SemesterRepository {
    List<Semester> findAll();

    Semester save(Semester semester);

    Semester findById(Long id);

    Semester getCurrent();

    void delete(Long id);
}
