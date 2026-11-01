package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.StoredEventStatusDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoredEventQueryService {
    StoredEventStatusDto isEventStored(Long eventId, String email);
    Page<EventInfoDto> getStoredEvents(String email, Pageable pageable);
}
