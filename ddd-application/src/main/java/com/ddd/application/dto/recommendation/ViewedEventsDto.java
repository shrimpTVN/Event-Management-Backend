package com.ddd.application.dto.recommendation;

import java.util.List;
import java.util.UUID;

public record ViewedEventsDto(UUID batchId, List<Long> eventIds) {}
