package com.ddd.api.dto.event.res;

public record PointCategoryGainResponseDto(
        Long categoryId,
        String categoryName,
        int pointsOffered,
        int effectiveGain
) {}

