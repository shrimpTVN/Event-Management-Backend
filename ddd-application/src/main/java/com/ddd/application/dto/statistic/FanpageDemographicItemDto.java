package com.ddd.application.dto.statistic;

public record FanpageDemographicItemDto(
        String label,
        long count,
        double percentage
) {}
