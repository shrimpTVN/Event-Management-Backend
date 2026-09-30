package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
import com.ddd.domain.repository.EventRepository;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventPointJpaEntity;
import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
import com.ddd.infrastructure.mapper.EventMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventRepositoryImpl implements EventRepository {

    private final EventJpaRepository eventJpaRepository;
    private final EventMapper eventMapper;
    private final EntityManager entityManager;

    @Override
    public Event save(Event event) {
        EventJpaEntity entity = eventMapper.toJpaEntity(event);

        if (event.getEventTypeId() != null) {
            EventTypeJpaEntity eventTypeRef = entityManager.getReference(EventTypeJpaEntity.class, event.getEventTypeId());
            entity.setEventType(eventTypeRef);
        }

        if (event.getCriteriaId() != null) {
            CriteriaJpaEntity criteriaRef = entityManager.getReference(CriteriaJpaEntity.class, event.getCriteriaId());
            entity.setCriteria(criteriaRef);
        } else {
            entity.setCriteria(null);
        }

        if (event.getFanpageId() != null) {
            FanpageJpaEntity fanpageRef = entityManager.getReference(FanpageJpaEntity.class, event.getFanpageId());
            entity.setFanpage(fanpageRef);
        }

        if (event.getSemesterId() != null) {
            SemesterJpaEntity semesterRef = entityManager.getReference(SemesterJpaEntity.class, event.getSemesterId());
            entity.setSemester(semesterRef);
        } else {
            entity.setSemester(null);
        }

        if (event.getEventPoints() != null && !event.getEventPoints().isEmpty()) {
            for (EventPoint ep : event.getEventPoints()) {
                EventPointJpaEntity epEntity = new EventPointJpaEntity();
                epEntity.setEvent(entity);

                if (ep.getPointCategoryId() != null) {
                    PointCategoryJpaEntity pointCategoryRef = entityManager
                            .getReference(PointCategoryJpaEntity.class, ep.getPointCategoryId());

                    epEntity.setPointCategory(pointCategoryRef);
                }
                epEntity.setPoint(ep.getPoint());
                entity.getEventPoints().add(epEntity);
            }
        }

        EventJpaEntity savedEntity = eventJpaRepository.save(entity);
        return eventMapper.toDomainModel(savedEntity);
    }

    @Override
    public Optional<Event> findById(Long id) {
        return eventJpaRepository.findById(id).map(eventMapper::toDomainModel);
    }

    @Override
    public Page<Event> findByFanpageId(Long fanpageId, Pageable pageable) {
        return eventJpaRepository.findByFanpage_Id(fanpageId, pageable)
                .map(eventMapper::toDomainModel);
    }
}
