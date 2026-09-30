package com.ddd.domain.repository;

import com.ddd.domain.model.Event;
import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(Long id);
}
