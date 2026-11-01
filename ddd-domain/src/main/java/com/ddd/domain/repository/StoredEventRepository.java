package com.ddd.domain.repository;

import com.ddd.domain.model.StoredEvent;

import java.util.Optional;

public interface StoredEventRepository {
    StoredEvent save(StoredEvent storedEvent);
    Optional<StoredEvent> findByUserIdAndEventId(Long userId, Long eventId);
    boolean existsByUserIdAndEventId(Long userId, Long eventId);
    void deleteByUserIdAndEventId(Long userId, Long eventId);
}
