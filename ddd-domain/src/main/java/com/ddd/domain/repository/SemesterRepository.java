package com.ddd.domain.repository;

import com.ddd.domain.model.Semester;

import java.util.List;

public interface SemesterRepository {
    List<Semester> findAll();
}
