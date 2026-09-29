package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointCategoryJpaRepository extends JpaRepository<PointCategoryJpaEntity, Long> {
    List<PointCategoryJpaEntity> findByLevel(Integer level, Sort sort);

    List<PointCategoryJpaEntity> findByParentCategoryId(Long parentId, Sort sort);

    List<PointCategoryJpaEntity> findByParentCategoryIsNull(Sort sort);
}
