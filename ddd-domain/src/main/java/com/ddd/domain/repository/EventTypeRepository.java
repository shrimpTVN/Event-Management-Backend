package com.ddd.domain.repository;

import com.ddd.domain.model.EventType;
import java.util.List;

public interface EventTypeRepository {
    List<EventType> findAll();
    EventType findById(Long id);
    EventType save(EventType eventType);
    EventType updateEventType(Long id, EventType eventType);
    void deleteEventType(Long id);
}
