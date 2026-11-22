package com.ddd.application.dto.statistic.admin;

public record AdminFanpageOverviewDto(
        long totalFanpages,
        long activeFanpages,
        long pendingFanpages,
        long bannedFanpages
) {}
