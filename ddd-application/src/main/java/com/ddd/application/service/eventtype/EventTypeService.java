package com.ddd.application.service.eventtype;

import com.ddd.application.dto.eventtype.EventTypeDto;

import java.util.List;

public interface EventTypeService {
    List<EventTypeDto> getAllEventTypes();
    EventTypeDto getEventTypeById(Long id);
    EventTypeDto createEventType(EventTypeDto dto);
    EventTypeDto updateEventType(Long id, EventTypeDto dto);
    void deleteEventType(Long id);
}
