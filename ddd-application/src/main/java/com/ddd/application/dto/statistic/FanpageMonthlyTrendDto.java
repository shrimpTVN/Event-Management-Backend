package com.ddd.application.dto.statistic;

public record FanpageMonthlyTrendDto(
        String monthYear,
        long eventCount,
        long registrationCount,
        long attendeeCount
) {}
