package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventTypeJpaRepository extends JpaRepository<EventTypeJpaEntity, Long> {
    List<EventTypeJpaEntity> findByIsActiveTrue();
}
