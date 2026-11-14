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
) {}

