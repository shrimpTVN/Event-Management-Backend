package com.ddd.application.service.event;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventSearchCriteriaDto;
import org.springframework.data.domain.Page;

public interface EventQueryService {
    EventInfoDto getEventById(Long id);
    Page<EventInfoDto> getAllEventsByFanpageId(Long fanpageId, int page, int size, String sortBy, String sortDir);

    Page<EventInfoDto> getAllEvents(int page, int size, String sortBy, String sortDir);

    Page<EventInfoDto> searchAndFilterEvents(EventSearchCriteriaDto eventSearchCriteriaDto);

    Page<EventInfoDto> getRecommendEvents(String email);
}
