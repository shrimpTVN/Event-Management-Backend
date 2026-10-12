package com.ddd.infrastructure.repository;

import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Registration;
import com.ddd.domain.repository.RegistrationRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
                    com.ddd.infrastructure.entity.RegistrationId regId = new com.ddd.infrastructure.entity.RegistrationId();
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
    public Registration findByUserAndEvent(Long userId, Long eventId) {
        RegistrationJpaEntity registrationJpaEntity = registrationJpaRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Not found Registration"));

        return registrationMapper.toDomain(registrationJpaEntity);

    }
}

