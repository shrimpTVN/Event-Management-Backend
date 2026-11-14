package com.ddd.domain.model.recommendation;

import java.util.Map;
import java.util.Set;

public record StudentRecommendationContext(
        Long userId,
        String email,
        Map<Long, Integer> missingPointGaps,
        Set<Long> missingCriteriaIds
) {}

