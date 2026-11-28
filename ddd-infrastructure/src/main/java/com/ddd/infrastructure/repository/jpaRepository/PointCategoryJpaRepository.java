package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PointCategoryJpaRepository extends JpaRepository<PointCategoryJpaEntity, Long> {
    List<PointCategoryJpaEntity> findByLevel(Integer level, Sort sort);

    List<PointCategoryJpaEntity> findByParentCategoryId(Long parentId, Sort sort);

    List<PointCategoryJpaEntity> findByParentCategoryIsNull(Sort sort);

    List<PointCategoryJpaEntity>  findAllByIsActive(boolean b, Sort defaultSort);

    List<PointCategoryJpaEntity> findAllByIsActiveAndDateApply(boolean isActive, LocalDate dateApply, Sort sort);

    @Query("SELECT MAX(p.dateApply) FROM PointCategoryJpaEntity p WHERE p.isActive = true AND p.dateApply <= :targetDate")
    Optional<LocalDate> findLatestEffectiveDateApplyAsOf(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT MIN(p.dateApply) FROM PointCategoryJpaEntity p WHERE p.isActive = true")
    Optional<LocalDate> findEarliestActiveDateApply();

    boolean existsByParentCategoryIdAndIsActive(Long pointCategoryId, boolean b);
}
