package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.infrastructure.entity.EventJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long>, JpaSpecificationExecutor<EventJpaEntity> {
    Page<EventJpaEntity> findByFanpage_Id(Long fanpageId, Pageable pageable);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Optional<EventJpaEntity> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Optional<EventJpaEntity> findWithDetailsByIdAndIsActiveTrue(Long id);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Page<EventJpaEntity> findWithDetailsByFanpage_Id(Long fanpageId, Pageable pageable);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester"})
    Page<EventJpaEntity> findWithDetailsByFanpage_IdAndIsActiveTrue(Long fanpageId, Pageable pageable);

    boolean existsByIdAndFanpageId(Long eventId, Long fanpageId);

    @EntityGraph(attributePaths = {"eventType", "criteria", "fanpage", "semester", "eventPoints", "eventPoints.pointCategory"})
    @Query("SELECT DISTINCT e FROM EventJpaEntity e WHERE e.status = :status AND e.isActive = true " +
           "AND e.dateHappen >= :today AND e.dateClose >= :today " +
           "AND (e.dateOpen IS NULL OR e.dateOpen <= :today)")
    List<EventJpaEntity> findUpcomingPublishedEvents(
            @Param("status") EventStatusEnum status,
            @Param("today") Instant today
    );
}
