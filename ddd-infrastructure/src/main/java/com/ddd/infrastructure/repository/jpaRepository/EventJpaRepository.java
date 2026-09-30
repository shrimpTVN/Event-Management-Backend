package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

@Repository
public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long> {
    Page<EventJpaEntity> findByFanpage_Id(Long fanpageId, Pageable pageable);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Optional<EventJpaEntity> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Page<EventJpaEntity> findWithDetailsByFanpage_Id(Long fanpageId, Pageable pageable);
}
