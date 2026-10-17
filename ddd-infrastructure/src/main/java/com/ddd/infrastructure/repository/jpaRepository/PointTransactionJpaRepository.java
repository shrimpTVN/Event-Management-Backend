package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.PointTransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionJpaRepository extends JpaRepository<PointTransactionJpaEntity, Long> {
}
