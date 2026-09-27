package com.ddd.infrastructure.repository;

import com.ddd.domain.model.EventType;
import com.ddd.domain.repository.EventTypeRepository;
import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import com.ddd.infrastructure.mapper.EventTypeMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventTypeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventTypeRepositoryImpl implements EventTypeRepository {
    private final EventTypeJpaRepository jpaRepository;
    private final EventTypeMapper mapper;

    @Override
    public List<EventType> findAll() {
        return jpaRepository.findByIsActiveTrue()
                .stream().map(mapper::toDomainModel).toList();
    }

    @Override
    public EventType findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomainModel)
                .orElseThrow(() -> new RuntimeException("EventType not found"));
    }

    @Override
    public EventType save(EventType eventType) {
        EventTypeJpaEntity entity = mapper.toJpaEntity(eventType);
        return mapper.toDomainModel(jpaRepository.save(entity));
    }

    @Override
    public EventType updateEventType(Long id, EventType eventType) {
        EventTypeJpaEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("EventType not found"));
        mapper.updateEntityFromDomain(eventType, entity);
        return mapper.toDomainModel(jpaRepository.save(entity));
    }

    @Override
    public void deleteEventType(Long id) {
        EventTypeJpaEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("EventType not found"));
        entity.setIsActive(false);
        jpaRepository.save(entity);
    }
}
