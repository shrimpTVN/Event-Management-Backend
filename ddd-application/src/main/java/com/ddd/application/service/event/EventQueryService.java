package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventInfoDto;
import org.springframework.data.domain.Page;

public interface EventQueryService {
    EventInfoDto getEventById(Long id);
    Page<EventInfoDto> getAllEventsByFanpageId(Long fanpageId, int page, int size, String sortBy, String sortDir);
}
