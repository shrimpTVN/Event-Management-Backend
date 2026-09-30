package com.ddd.domain.repository;

import com.ddd.domain.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EventRepository {
    Event save(Event event);
    Optional<Event> findById(Long id);
    Page<Event> findByFanpageId(Long fanpageId, Pageable pageable);
}
