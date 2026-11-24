package com.ddd.api.dto.event.res;

import java.util.List;

public record EventRecommendationResponseDto(
        EventResponseDto event,
        String explain,
        boolean viewed,
        int remainingSlots,
        long daysUntilHappen,
        boolean fulfillsCriteria,
        List<PointCategoryGainResponseDto> pointGains,
        double matchScore,
        double personalizedScore,
        double urgencyScore,
        int readPenalty,
        double effectiveScore
) {}

