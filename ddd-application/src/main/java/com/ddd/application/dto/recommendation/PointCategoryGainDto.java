package com.ddd.application.dto.recommendation;

public record PointCategoryGainDto(
        Long categoryId,
        String categoryName,
        int pointsOffered,
        int effectiveGain
) {}

