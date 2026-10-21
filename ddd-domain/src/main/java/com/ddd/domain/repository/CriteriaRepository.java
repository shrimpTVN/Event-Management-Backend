package com.ddd.domain.repository;

import com.ddd.domain.model.Criteria;

import java.util.List;
import java.util.Optional;

public interface CriteriaRepository {
    Criteria save(Criteria criteria);
    Optional<Criteria> findById(Long id);
    List<Criteria> findAll(boolean isActive);
    List<Criteria> findByParentId(Long parentId);
    List<Criteria> findByLevel(Integer level);
    void delete(Criteria criteria);

    boolean hasChildren(Long criteriaId);
}
