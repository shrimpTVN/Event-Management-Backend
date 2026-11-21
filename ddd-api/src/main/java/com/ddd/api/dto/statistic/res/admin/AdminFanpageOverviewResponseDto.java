package com.ddd.api.dto.statistic.res.admin;

public record AdminFanpageOverviewResponseDto(
        long totalFanpages,
        long activeFanpages,
        long pendingFanpages,
        long bannedFanpages
) {}
