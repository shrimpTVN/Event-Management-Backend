package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionJpaEntity, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user", "semester", "pointCategory", "event"})
    List<PointTransactionJpaEntity> findByUserIdInAndSemesterId(List<Long> userIds, Long semesterId);

    List<PointTransactionJpaEntity> findByUserIdAndSemesterIdAndPointCategoryId(Long id,
                                                                                Long semesterId,
                                                                                Long pointCategoryId);
}
