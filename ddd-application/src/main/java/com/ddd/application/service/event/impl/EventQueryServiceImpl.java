package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventQueryServiceImpl implements EventQueryService {

    private final EventJpaRepository eventJpaRepository;
    private final EventDtoMapper eventDtoMapper;

    @Override
    public EventInfoDto getEventById(Long id) {
        EventJpaEntity eventJpaEntity = eventJpaRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        return eventDtoMapper.toInfoDto(eventJpaEntity);
    }

    @Override
    public Page<EventInfoDto> getAllEventsByFanpageId(Long fanpageId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        
        return eventJpaRepository.findWithDetailsByFanpage_Id(fanpageId, pageable)
                .map(eventDtoMapper::toInfoDto);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    @Override
    public Page<EventInfoDto> getAllEvents(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);

        return eventJpaRepository.findAll(pageable)
                .map(eventDtoMapper::toInfoDto);
    }}
