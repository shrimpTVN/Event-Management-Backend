package com.ddd.application.dto.recommendation;

import com.ddd.application.dto.event.EventInfoDto;

import java.util.List;

public record EventRecommendationDto(
        EventInfoDto event,
        double matchScore,
        String aiExplanation,
        int remainingSlots,
        long daysUntilHappen,
        boolean fulfillsCriteria,
        List<PointCategoryGainDto> pointGains,
        boolean isFollowedFanpage,
        double fanpageScore,
        double historyScore
) {
    public EventRecommendationDto(
            EventInfoDto event,
            double matchScore,
            String aiExplanation,
            int remainingSlots,
            long daysUntilHappen,
            boolean fulfillsCriteria,
            List<PointCategoryGainDto> pointGains
    ) {
        this(event, matchScore, aiExplanation, remainingSlots, daysUntilHappen, fulfillsCriteria, pointGains, false, 0.0, 0.0);
    }
}

