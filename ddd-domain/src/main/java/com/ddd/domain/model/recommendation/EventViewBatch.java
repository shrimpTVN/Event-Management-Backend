package com.ddd.domain.model.recommendation;

import java.util.List;
import java.util.UUID;

public record EventViewBatch(UUID batchId, List<Long> eventIds) {
    public EventViewBatch {
        eventIds = List.copyOf(eventIds);
    }
}
