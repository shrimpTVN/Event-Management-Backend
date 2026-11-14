package com.ddd.api.dto.statistic.res;

public record FanpageMonthlyTrendResponseDto(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount
) {}
