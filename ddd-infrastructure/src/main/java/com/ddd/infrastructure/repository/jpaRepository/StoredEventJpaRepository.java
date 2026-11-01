package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.StoredEventJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredEventJpaRepository extends JpaRepository<StoredEventJpaEntity, Long> {

    Optional<StoredEventJpaEntity> findByUser_IdAndEvent_Id(Long userId, Long eventId);

    boolean existsByUser_IdAndEvent_Id(Long userId, Long eventId);

    @Modifying
    @Query("DELETE FROM StoredEventJpaEntity s WHERE s.user.id = :userId AND s.event.id = :eventId")
    void deleteByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @EntityGraph(attributePaths = {"event", "event.eventType", "event.criteria", "event.fanpage", "event.semester"})
    @Query(
            value = "SELECT s.event FROM StoredEventJpaEntity s " +
                    "WHERE s.user.id = :userId " +
                    "AND s.event.isActive = true",
            countQuery = "SELECT COUNT(s) FROM StoredEventJpaEntity s " +
                    "WHERE s.user.id = :userId " +
                    "AND s.event.isActive = true"
    )
    Page<EventJpaEntity> findStoredEventsByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
