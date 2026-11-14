package com.ddd.infrastructure.dto.row.event;

public record EventStatusFunnelRow(
        long registeredCount,
        long selfCheckedInCount,
        long checkedInCount,
        long spendingConfirmCount,
        long confirmedCount,
        long completedCount,
        long absentCount,
        long cancelledCount
) {
}
