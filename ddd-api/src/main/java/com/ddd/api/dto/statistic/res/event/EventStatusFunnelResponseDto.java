package com.ddd.api.dto.statistic.res.event;

public record EventStatusFunnelResponseDto(
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
