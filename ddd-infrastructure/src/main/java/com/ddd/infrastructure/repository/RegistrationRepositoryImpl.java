package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Registration;
import com.ddd.domain.repository.RegistrationRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.RegistrationId;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RegistrationRepositoryImpl implements RegistrationRepository {
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationMapper registrationMapper;
    private final EntityManager entityManager;

    @Override
    public boolean isRegistered(Long userId, Long eventId) {
        return registrationJpaRepository.existsById(new RegistrationId(userId, eventId));
    }

    @Override
    public void save(Registration registration, Long userId, Long eventId) {
        RegistrationId regId = new RegistrationId(userId, eventId);

        RegistrationJpaEntity registrationJpaEntity = registrationJpaRepository.findById(regId)
                .orElseGet(() -> {
                    RegistrationJpaEntity newEntity = new RegistrationJpaEntity();
                    newEntity.setId(regId);
                    newEntity.setUser(entityManager.getReference(UserJpaEntity.class, userId));
                    newEntity.setEvent(entityManager.getReference(EventJpaEntity.class, eventId));
                    return newEntity;
                });

        registrationMapper.updateEntityFromDomain(registration, registrationJpaEntity);
        registrationJpaRepository.save(registrationJpaEntity);
    }

    @Override
    public void saveAll(List<Registration> registrations, Long eventId) {
        if (registrations == null || registrations.isEmpty()) {
            return;
        }

        List<Long> userIds = registrations.stream()
                .map(Registration::getUserId)
                .toList();

        Map<Long, RegistrationJpaEntity> existingEntities = registrationJpaRepository
                .findByEventIdAndUserIdIn(eventId, userIds).stream()
                .collect(Collectors.toMap(
                        e -> e.getId().getUserId(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        for (Registration reg : registrations) {
            RegistrationJpaEntity entity = existingEntities.get(reg.getUserId());
            if (entity == null) {
                throw new ResourceNotFoundException(
                        "Cannot batch update: Registration not found for user " + reg.getUserId() + " in event " + eventId
                );
            }
            registrationMapper.updateEntityFromDomain(reg, entity);
        }

        registrationJpaRepository.saveAll(existingEntities.values());
    }

    @Override
    public Registration findByUserAndEvent(Long userId, Long eventId) {
        RegistrationId regId = new RegistrationId(userId, eventId);
        RegistrationJpaEntity registrationJpaEntity = registrationJpaRepository.findById(regId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Registration with userId: " + userId + " and eventId: " + eventId));

        return registrationMapper.toDomain(registrationJpaEntity);
    }

    @Override
    public List<Registration> findAllByUserIdsAndEvent(Collection<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return registrationJpaRepository.findByEventIdAndUserIdIn(eventId, userIds).stream()
                .map(registrationMapper::toDomain)
                .toList();
    }
}
