package com.ddd.application.service.eventtype.impl;

import com.ddd.application.dto.eventtype.EventTypeDto;
import com.ddd.application.mapper.EventTypeDtoMapper;
import com.ddd.application.service.eventtype.EventTypeService;
import com.ddd.domain.model.EventType;
import com.ddd.domain.repository.EventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventTypeServiceImpl implements EventTypeService {

    private final EventTypeRepository repository;
    private final EventTypeDtoMapper mapper;

    @Override
    public List<EventTypeDto> getAllEventTypes() {
        return repository.findAll().stream().map(mapper::toDto).collect(Collectors.toList());
    }

    @Override
    public EventTypeDto getEventTypeById(Long id) {
        return mapper.toDto(repository.findById(id));
    }

    @Override
    @Transactional
    public EventTypeDto createEventType(EventTypeDto dto) {
        EventType domain = mapper.toDomain(dto);
        EventType saved = repository.save(domain);
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public EventTypeDto updateEventType(Long id, EventTypeDto dto) {
        EventType domain = mapper.toDomain(dto);
        EventType updated = repository.updateEventType(id, domain);
        return mapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteEventType(Long id) {
        repository.deleteEventType(id);
    }
}
