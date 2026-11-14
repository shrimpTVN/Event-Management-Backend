package com.ddd.api.dto.statistic.res.fanpage;

public record FanpageEventDistributionResponseDto(
        String eventTypeName,
        long eventCount,
        double percentage
) {}
