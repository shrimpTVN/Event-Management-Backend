package com.ddd.infrastructure.repository;

import com.ddd.domain.model.EventPoint;
import com.ddd.domain.repository.EventPointRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventPointJpaEntity;
import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import com.ddd.infrastructure.mapper.EventPointMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventPointJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventPointRepositoryImpl implements EventPointRepository {

    private final EventPointJpaRepository eventPointJpaRepository;
    private final EventPointMapper eventPointMapper;
    private final EntityManager entityManager;

    @Override
    public void saveAll(List<EventPoint> eventPoints) {
        if (eventPoints == null || eventPoints.isEmpty()) {
            return;
        }

        // Map and associate foreign key references using EntityManager proxies
        List<EventPointJpaEntity> eventPointJpaEntities = eventPoints.stream()
                .map(eventPoint -> {
                    EventPointJpaEntity entity = eventPointMapper.toJpaEntity(eventPoint);

                    if (eventPoint.getEventId() != null) {
                        EventJpaEntity eventRef = entityManager.getReference(EventJpaEntity.class, eventPoint.getEventId());
                        entity.setEvent(eventRef);
                    }

                    if (eventPoint.getPointCategoryId() != null) {
                        PointCategoryJpaEntity pointCategoryRef = entityManager.getReference(PointCategoryJpaEntity.class, eventPoint.getPointCategoryId());
                        entity.setPointCategory(pointCategoryRef);
                    }

                    return entity;
                })
                .toList();

        // Persist all entities together in batch
        eventPointJpaRepository.saveAll(eventPointJpaEntities);
    }

    @Override
    public List<EventPoint> findByEventId(Long eventId) {
        return eventPointJpaRepository.findByEvent_Id(eventId).stream()
                .map(eventPointMapper::toDomainModel)
                .toList();
    }
}
