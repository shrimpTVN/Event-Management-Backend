package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, Long> {

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Optional<RegistrationJpaEntity> findByUserIdAndEventId(Long userId, Long eventId);

    Page<RegistrationJpaEntity> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT r FROM RegistrationJpaEntity r JOIN FETCH r.event e " +
            "WHERE r.user.id = :userId " +
            "AND r.status = com.ddd.domain.enums.RegistrationStatusEnum.REGISTERED " +
            "AND e.dateHappen > CURRENT_TIMESTAMP")
    List<RegistrationJpaEntity> findUpcomingRegisteredEvents(@Param("userId") Long userId);
}
