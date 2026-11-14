package com.ddd.api.dto.statistic.res.event;

public record EventCheckInTimelineResponseDto(
        String timeSlot,
        long checkInCount
) {
}
