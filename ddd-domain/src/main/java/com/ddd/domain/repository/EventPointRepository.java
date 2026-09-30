package com.ddd.domain.repository;

import com.ddd.domain.model.EventPoint;

import java.util.List;

public interface EventPointRepository {
    void saveAll(List<EventPoint> eventPoints);
    List<EventPoint> findByEventId(Long eventId);
}
