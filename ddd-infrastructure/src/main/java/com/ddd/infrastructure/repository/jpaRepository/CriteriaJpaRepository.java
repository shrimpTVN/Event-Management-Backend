package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CriteriaJpaRepository extends JpaRepository<CriteriaJpaEntity, Long> {
    List<CriteriaJpaEntity> findAllByIsActive(boolean isActive, Sort sort);
    List<CriteriaJpaEntity> findAllByIsActive(boolean isActive);
    List<CriteriaJpaEntity> findByParentIsNull(Sort sort);
    List<CriteriaJpaEntity> findByParentId(Long parentId, Sort sort);
    List<CriteriaJpaEntity> findByLevel(Integer level, Sort sort);
}
