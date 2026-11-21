package com.ddd.application.dto.statistic;

public record EventCheckInTimelineDto(
        String timeSlot,
        long checkInCount
) {
}
