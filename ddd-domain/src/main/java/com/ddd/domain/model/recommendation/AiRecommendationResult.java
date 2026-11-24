package com.ddd.domain.model.recommendation;

public record AiRecommendationResult(
        Long eventId,
        double aiScore,
        String aiExplanation
) {}

