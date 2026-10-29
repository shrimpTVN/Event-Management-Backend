package com.ddd.infrastructure.repository;

import com.ddd.domain.model.StoredEvent;
import com.ddd.domain.repository.StoredEventRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.StoredEventJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.StoredEventMapper;
import com.ddd.infrastructure.repository.jpaRepository.StoredEventJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StoredEventRepositoryImpl implements StoredEventRepository {

    private final StoredEventJpaRepository storedEventJpaRepository;
    private final StoredEventMapper storedEventMapper;
    private final EntityManager entityManager;

    @Override
    public StoredEvent save(StoredEvent domainModel) {
        StoredEventJpaEntity entity = storedEventMapper.toJpaEntity(domainModel);
        if (domainModel.getId() != null) {
            entity.setId(domainModel.getId());
        }

        UserJpaEntity userRef = entityManager.getReference(UserJpaEntity.class, domainModel.getUserId());
        EventJpaEntity eventRef = entityManager.getReference(EventJpaEntity.class, domainModel.getEventId());
        entity.setUser(userRef);
        entity.setEvent(eventRef);

        StoredEventJpaEntity saved = storedEventJpaRepository.save(entity);
        return storedEventMapper.toDomain(saved);
    }

    @Override
    public Optional<StoredEvent> findByUserIdAndEventId(Long userId, Long eventId) {
        return storedEventJpaRepository.findByUser_IdAndEvent_Id(userId, eventId)
                .map(storedEventMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndEventId(Long userId, Long eventId) {
        return storedEventJpaRepository.existsByUser_IdAndEvent_Id(userId, eventId);
    }

    @Override
    public void deleteByUserIdAndEventId(Long userId, Long eventId) {
        storedEventJpaRepository.deleteByUserIdAndEventId(userId, eventId);
    }
}
