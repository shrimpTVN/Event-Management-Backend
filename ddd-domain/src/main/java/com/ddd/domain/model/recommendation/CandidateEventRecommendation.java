package com.ddd.domain.model.recommendation;

public record CandidateEventRecommendation(
        Long eventId,
        String title,
        String description,
        int remainingSlots,
        long daysUntilHappen,
        int totalEffectivePoints,
        boolean fulfillsCriteria,
        String criteriaName,
        double baseScore,
        boolean isFollowedFanpage,
        double historyScore
) {
    public CandidateEventRecommendation(
            Long eventId,
            String title,
            String description,
            int remainingSlots,
            long daysUntilHappen,
            int totalEffectivePoints,
            boolean fulfillsCriteria,
            String criteriaName,
            double baseScore
    ) {
        this(eventId, title, description, remainingSlots, daysUntilHappen, totalEffectivePoints, fulfillsCriteria, criteriaName, baseScore, false, 0.0);
    }
}

