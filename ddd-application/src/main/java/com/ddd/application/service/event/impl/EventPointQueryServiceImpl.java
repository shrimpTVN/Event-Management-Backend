package com.ddd.application.service.event.impl;

import com.ddd.application.dto.event.EventPointInfoDto;
import com.ddd.application.mapper.EventDtoMapper;
import com.ddd.application.service.event.EventPointQueryService;
import com.ddd.domain.repository.EventPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPointQueryServiceImpl implements EventPointQueryService {

    private final EventPointRepository eventPointRepository;
    private final EventDtoMapper eventDtoMapper;

    @Override
    public List<EventPointInfoDto> getEventPointsByEventId(Long eventId) {
        return eventPointRepository.findByEventId(eventId).stream()
                .map(eventDtoMapper::toEventPointInfoDto)
                .toList();
    }
}
