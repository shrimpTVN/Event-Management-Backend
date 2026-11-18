package com.ddd.application.dto.statistic;

public record EventDemographicItemDto(
        String label,
        long count,
        double percentage
) {
}
