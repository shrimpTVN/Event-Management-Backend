package com.ddd.api.dto.statistic.res.fanpage;

public record FanpageMonthlyTrendResponseDto(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount
) {}
