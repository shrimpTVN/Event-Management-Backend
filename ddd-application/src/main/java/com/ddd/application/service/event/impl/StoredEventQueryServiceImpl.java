package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.StoredEventStatusDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.StoredEventQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StoredEventJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class StoredEventQueryServiceImpl implements StoredEventQueryService {

    private final UserRepository userRepository;
    private final StoredEventJpaRepository storedEventJpaRepository;
    private final EventDtoMapper eventDtoMapper;

    @Override
    public StoredEventStatusDto isEventStored(Long eventId, String email) {
        log.info("Checking stored status for event id: {} by user email: {}", eventId, email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        boolean isStored = storedEventJpaRepository.existsByUser_IdAndEvent_Id(user.getId(), eventId);
        return new StoredEventStatusDto(eventId, isStored);
    }

    @Override
    public Page<EventInfoDto> getStoredEvents(String email, Pageable pageable) {
        log.info("Fetching stored events for user email: {}", email);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        Page<EventJpaEntity> events = storedEventJpaRepository.findStoredEventsByUserId(user.getId(), pageable);
        return events.map(eventDtoMapper::toInfoDto);
    }
}
