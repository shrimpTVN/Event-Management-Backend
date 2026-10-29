package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Registration;
import com.ddd.domain.repository.RegistrationRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.RegistrationId;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RegistrationRepositoryImpl implements RegistrationRepository {
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationMapper registrationMapper;
    private final UserJpaRepository userJpaRepository;
    private final EventJpaRepository eventJpaRepository;

    @Override
    public boolean isRegistered(Long userId, Long eventId) {
        return registrationJpaRepository.existsByUserIdAndEventId(userId, eventId);
    }

    @Override
    public void save(Registration registration, Long userId, Long eventId) {
        UserJpaEntity userJpaEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found user"));
        EventJpaEntity eventJpaEntity = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Event"));

        RegistrationJpaEntity registrationJpaEntity = registrationJpaRepository.findByUserIdAndEventId(userId, eventId)
                .orElseGet(() -> {
                    RegistrationJpaEntity newEntity = new RegistrationJpaEntity();
                    RegistrationId regId = new RegistrationId();
                    regId.setUserId(userId);
                    regId.setEventId(eventId);

                    newEntity.setId(regId);
                    newEntity.setUser(userJpaEntity);
                    newEntity.setEvent(eventJpaEntity);
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
                .collect(Collectors.toMap(e -> e.getUser().getId(), e -> e));

        for (Registration reg : registrations) {
            RegistrationJpaEntity entity = existingEntities.get(reg.getUserId());
            registrationMapper.updateEntityFromDomain(reg, entity);
        }

        registrationJpaRepository.saveAll(existingEntities.values());
    }

    @Override
    public Registration findByUserAndEvent(Long userId, Long eventId) {
        RegistrationJpaEntity registrationJpaEntity = registrationJpaRepository.findByUserIdAndEventId(userId, eventId)
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

