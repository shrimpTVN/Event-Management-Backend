package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Event;
import com.ddd.domain.model.EventPoint;
import com.ddd.domain.repository.EventRepository;
import com.ddd.infrastructure.entity.*;
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
        // Initialize or update the base entity
        EventJpaEntity entity = prepareEntityForSave(event);

        // Resolve and set JPA proxies for related aggregates
        setAssociationReferences(event, entity);

        //Synchronize the EventPoints collection
        syncEventPoints(event, entity);

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

    private EventJpaEntity prepareEntityForSave(Event event) {
        if (event.getId() == null) {
            return eventMapper.toJpaEntity(event);
        }

        EventJpaEntity entity = eventJpaRepository.findById(event.getId())
                .orElseThrow(() -> new IllegalArgumentException("Event with ID " + event.getId() + " not found"));

        entity.getEventPoints().clear();
        entityManager.flush(); // Execute DELETE statements immediately to free unique keys
        eventMapper.updateJpaEntity(event, entity);

        return entity;
    }

    private void setAssociationReferences(Event event, EventJpaEntity entity) {
        if (event.getEventTypeId() == null || event.getSemesterId() == null || event.getFanpageId() == null) {
            throw new IllegalArgumentException("EventTypeId, SemesterId, and FanpageId must not be null");
        }

        entity.setEventType(entityManager.getReference(EventTypeJpaEntity.class, event.getEventTypeId()));
        entity.setFanpage(entityManager.getReference(FanpageJpaEntity.class, event.getFanpageId()));
        entity.setSemester(entityManager.getReference(SemesterJpaEntity.class, event.getSemesterId()));

        if (event.getCriteriaId() != null) {
            entity.setCriteria(entityManager.getReference(CriteriaJpaEntity.class, event.getCriteriaId()));
        } else {
            entity.setCriteria(null);
        }
    }

    private void syncEventPoints(Event event, EventJpaEntity entity) {
        if (event.getEventPoints() == null || event.getEventPoints().isEmpty()) {
            return;
        }

        for (EventPoint ep : event.getEventPoints()) {
            EventPointJpaEntity epEntity = new EventPointJpaEntity();
            epEntity.setEvent(entity);
            epEntity.setPoint(ep.getPoint());

            if (ep.getPointCategoryId() != null) {
                epEntity.setPointCategory(
                        entityManager.getReference(PointCategoryJpaEntity.class, ep.getPointCategoryId())
                );
            }

            entity.getEventPoints().add(epEntity);
        }
    }
}
