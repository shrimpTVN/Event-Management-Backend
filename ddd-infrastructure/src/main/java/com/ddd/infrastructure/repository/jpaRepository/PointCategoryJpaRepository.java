package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointCategoryJpaRepository extends JpaRepository<PointCategoryJpaEntity, Long> {
}
