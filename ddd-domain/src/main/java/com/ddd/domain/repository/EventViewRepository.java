package com.ddd.domain.repository;

import com.ddd.domain.model.recommendation.EventViewBatch;
import java.time.Instant;
import java.util.Map;

public interface EventViewRepository {
    void record(long studentId, EventViewBatch batch, Instant receivedAt);

    Map<Long, Integer> findPenalties(long studentId);
}
