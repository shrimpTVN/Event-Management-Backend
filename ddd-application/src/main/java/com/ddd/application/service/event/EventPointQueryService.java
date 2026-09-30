package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventPointInfoDto;
import java.util.List;

public interface EventPointQueryService {
    List<EventPointInfoDto> getEventPointsByEventId(Long eventId);
}
