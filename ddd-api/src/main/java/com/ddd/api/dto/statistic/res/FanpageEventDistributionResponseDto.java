package com.ddd.api.dto.statistic.res;

public record FanpageEventDistributionResponseDto(
        String eventTypeName,
        long eventCount,
        double percentage
) {}
