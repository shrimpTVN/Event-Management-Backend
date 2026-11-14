package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.RegistrationId;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, RegistrationId> {

    @Query("SELECT r FROM RegistrationJpaEntity r " +
            "WHERE r.id.eventId = :eventId " +
            "AND r.id.userId IN :userIds")
    List<RegistrationJpaEntity> findByEventIdAndUserIdIn(@Param("eventId") Long eventId, @Param("userIds") Collection<Long> userIds);

    @EntityGraph(attributePaths = {"event", "event.eventType", "event.criteria",
            "event.fanpage", "event.semester", "event.eventPoints"})
    Page<RegistrationJpaEntity> findAllByUserId(Long userId, Pageable pageable);


    @EntityGraph(attributePaths = {"event", "event.eventType", "event.criteria",
            "event.fanpage", "event.semester", "event.eventPoints"})
    @Query("SELECT r FROM RegistrationJpaEntity r JOIN FETCH r.event e " +
            "WHERE r.user.id = :userId " +
            "AND r.status = RegistrationStatusEnum.REGISTERED " +
            "AND e.dateHappen > CURRENT_TIMESTAMP")
    List<RegistrationJpaEntity> findUpcomingRegisteredEvents(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {"user", "user.studentProfile", "user.studentProfile.major"})
    Page<RegistrationJpaEntity> findAllByEventId(Long eventId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RegistrationJpaEntity r WHERE r.event.id = :eventId AND r.status <> :cancelledStatus")
    long countActiveRegistrationsByEventId(@Param("eventId") Long eventId, @Param("cancelledStatus") com.ddd.domain.enums.RegistrationStatusEnum cancelledStatus);

    @Query("SELECT r.event.id, COUNT(r) FROM RegistrationJpaEntity r " +
            "WHERE r.event.id IN :eventIds AND r.status <> :cancelledStatus " +
            "GROUP BY r.event.id")
    List<Object[]> countActiveRegistrationsByEventIds(
            @Param("eventIds") Collection<Long> eventIds,
            @Param("cancelledStatus") com.ddd.domain.enums.RegistrationStatusEnum cancelledStatus
    );

    @Query("SELECT r.event.eventType.id, COUNT(r) FROM RegistrationJpaEntity r " +
            "WHERE r.user.id = :userId " +
            "AND r.status IN :attendedStatuses " +
            "AND r.event.eventType IS NOT NULL " +
            "GROUP BY r.event.eventType.id")
    List<Object[]> countAttendedEventTypesByUserId(
            @Param("userId") Long userId,
            @Param("attendedStatuses") Collection<com.ddd.domain.enums.RegistrationStatusEnum> attendedStatuses
    );

    @Query("SELECT DISTINCT ep.pointCategory.id FROM RegistrationJpaEntity r " +
            "JOIN r.event.eventPoints ep " +
            "WHERE r.user.id = :userId " +
            "AND r.status IN :attendedStatuses " +
            "AND ep.pointCategory IS NOT NULL")
    List<Long> findAttendedPointCategoryIdsByUserId(
            @Param("userId") Long userId,
            @Param("attendedStatuses") Collection<com.ddd.domain.enums.RegistrationStatusEnum> attendedStatuses
    );

    @Query("SELECT r.event.id FROM RegistrationJpaEntity r WHERE r.user.id = :userId AND r.status <> :cancelledStatus")
    List<Long> findActiveRegisteredEventIdsByUserId(@Param("userId") Long userId, @Param("cancelledStatus") com.ddd.domain.enums.RegistrationStatusEnum cancelledStatus);
     @EntityGraph(attributePaths = {"user", "user.studentProfile", "user.studentProfile.major"})
    @Query("SELECT r FROM RegistrationJpaEntity r " +
            "LEFT JOIN r.user u " +
            "LEFT JOIN u.studentProfile sp " +
            "WHERE r.id.eventId = :eventId " +
            "ORDER BY sp.studentId ASC NULLS LAST")
    List<RegistrationJpaEntity> findAllByEventId(@Param("eventId") Long eventId);

     @EntityGraph(attributePaths = {"event"})
     @Query("SELECT r FROM RegistrationJpaEntity r " +
             "WHERE r.user.id = :userId " +
             "AND r.event.dateHappen BETWEEN :startTime AND :endTime")
    List<RegistrationJpaEntity> findArrangedEvents(@Param("userId") Long userId,
                                                   @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
