package com.ddd.infrastructure.dto.row.event;

public record CheckInTimelineRow(
        String timeSlot,
        long checkInCount
) {
}
