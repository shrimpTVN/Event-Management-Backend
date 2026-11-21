package com.ddd.api.dto.statistic.res.admin;

public record AdminTopFanpageResponseDto(
        Long fanpageId,
        String fanpageName,
        String avatarUrl,
        long eventCount,
        long followerCount,
        long attendeeCount
) {}
