package com.ddd.application.dto.fanpage;

public record FanpageFollowCountDto(
        Long fanpageId,
        long followerCount,
        Boolean isFollowing
) {}
