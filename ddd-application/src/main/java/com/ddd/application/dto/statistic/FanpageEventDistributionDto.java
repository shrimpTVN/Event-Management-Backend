package com.ddd.application.dto.statistic;

public record FanpageEventDistributionDto(
        String eventTypeName,
        long eventCount,
        double percentage
) {}
