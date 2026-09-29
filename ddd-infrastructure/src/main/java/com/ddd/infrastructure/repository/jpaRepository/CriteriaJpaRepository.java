package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriteriaJpaRepository extends JpaRepository<CriteriaJpaEntity, Long> {
    List<CriteriaJpaEntity> findAllByIsActive(boolean isActive);
}
