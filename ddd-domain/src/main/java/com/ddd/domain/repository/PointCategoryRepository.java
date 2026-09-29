package com.ddd.domain.repository;

import com.ddd.domain.model.PointCategory;

import java.util.List;
import java.util.Optional;

public interface PointCategoryRepository {
    Optional<PointCategory> findById(Long id);

    PointCategory save(PointCategory pointCategory);

    List<PointCategory> findAll();
}

