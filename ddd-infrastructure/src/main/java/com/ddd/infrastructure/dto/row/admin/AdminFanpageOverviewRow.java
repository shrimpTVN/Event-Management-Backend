package com.ddd.infrastructure.dto.row.admin;

public record AdminFanpageOverviewRow(
        long totalFanpages,
        long activeFanpages,
        long pendingFanpages,
        long bannedFanpages
) {}
