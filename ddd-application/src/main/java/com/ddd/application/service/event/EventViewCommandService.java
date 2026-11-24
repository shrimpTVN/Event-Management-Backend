package com.ddd.application.service.event;

import com.ddd.application.dto.recommendation.ViewedEventsDto;

public interface EventViewCommandService {
    void recordViews(ViewedEventsDto dto);
}
