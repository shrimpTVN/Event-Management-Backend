package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.EventPointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventPointJpaRepository extends JpaRepository<EventPointJpaEntity, String> {
    List<EventPointJpaEntity> findByEvent_Id(Long eventId);
}
