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
