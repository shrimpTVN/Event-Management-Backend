package com.ddd.application.dto.statistic.admin;

public record AdminTopFanpageDto(
        Long fanpageId,
        String fanpageName,
        String avatarUrl,
        long eventCount,
        long followerCount,
        long attendeeCount
) {}
