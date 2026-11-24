package com.ddd.api.dto.event.res;

import java.util.List;

public record EventRecommendationResponseDto(
        EventResponseDto event,
        double matchScore,
        String aiExplanation,
        int remainingSlots,
        long daysUntilHappen,
        boolean fulfillsCriteria,
        List<PointCategoryGainResponseDto> pointGains,
        boolean isFollowedFanpage,
        boolean registrationClosed,
        boolean full,
        boolean viewed,
        double personalizedScore,
        double urgencyScore,
        int readPenalty,
        double effectiveScore
) {}

