package com.ddd.domain.repository;

import com.ddd.domain.model.Criteria;

public interface CriteriaRepository {
    Criteria save(Criteria criteria);
    Criteria findById(Long id);
    void delete(Criteria criteria);
}
