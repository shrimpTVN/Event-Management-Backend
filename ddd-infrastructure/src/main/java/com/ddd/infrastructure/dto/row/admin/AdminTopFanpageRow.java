package com.ddd.infrastructure.dto.row.admin;

public record AdminTopFanpageRow(
        Long fanpageId,
        String fanpageName,
        String avatarUrl,
        long eventCount,
        long followerCount,
        long attendeeCount
) {}
