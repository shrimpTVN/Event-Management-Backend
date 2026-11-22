package com.ddd.domain.model.recommendation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record StudentRecommendationContext(
        Long userId,
        String email,
        Map<Long, Integer> missingPointGaps,
        Set<Long> missingCriteriaIds,
        Map<String, Integer> missingPointGapsNamed,
        List<String> missingCriteriaNames
) {
    public StudentRecommendationContext(
            Long userId,
            String email,
            Map<Long, Integer> missingPointGaps,
            Set<Long> missingCriteriaIds
    ) {
        this(userId, email, missingPointGaps, missingCriteriaIds, Map.of(), List.of());
    }
}
