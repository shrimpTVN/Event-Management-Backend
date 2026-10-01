package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.domain.enums.EventStatusEnum;

public interface EventCommandService {
    void createEvent(EventCreateDto dto, String email);
    void updateEvent(Long id, EventUpdateDto dto, String email);
    void changeEventStatus(Long id, EventStatusEnum status, String email);
    void deleteEvent(Long id, String email);
}
