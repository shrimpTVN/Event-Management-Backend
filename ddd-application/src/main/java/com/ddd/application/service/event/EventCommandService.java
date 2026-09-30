package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;

public interface EventCommandService {
    void createEvent(EventCreateDto dto, String email);
}
