package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionJpaEntity, Long> {

    List<PointTransactionJpaEntity> findByUserIdAndSemesterIdAndPointCategoryId(Long id,
                                                                                Long semesterId,
                                                                                Long pointCategoryId);
}
