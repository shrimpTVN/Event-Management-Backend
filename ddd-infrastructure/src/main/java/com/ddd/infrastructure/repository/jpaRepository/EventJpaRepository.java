package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long> {
}
