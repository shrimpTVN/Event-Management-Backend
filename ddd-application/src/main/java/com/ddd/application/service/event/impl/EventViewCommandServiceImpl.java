package com.ddd.application.service.event.impl;

import com.ddd.application.dto.recommendation.ViewedEventsDto;
import com.ddd.application.mapper.EventViewDtoMapper;
import com.ddd.application.service.event.EventViewCommandService;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.EventViewRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventViewCommandServiceImpl implements EventViewCommandService {
    private final SecurityContextPort security;
    private final EventViewDtoMapper mapper;
    private final EventViewRepository repository;

    @Override
    @Transactional
    public void recordViews(ViewedEventsDto dto) {
        Long studentId = security.getCurrentUserId();
        if (!security.isAuthenticated() || studentId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        if (!dto.eventIds().isEmpty()) {
            repository.record(studentId, mapper.toDomain(dto), Instant.now());
        }
    }
}
