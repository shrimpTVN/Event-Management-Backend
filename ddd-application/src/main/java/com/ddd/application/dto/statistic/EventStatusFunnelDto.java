package com.ddd.application.dto.statistic;

public record EventStatusFunnelDto(
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
