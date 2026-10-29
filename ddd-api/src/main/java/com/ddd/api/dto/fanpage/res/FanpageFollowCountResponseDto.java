package com.ddd.api.dto.fanpage.res;

public record FanpageFollowCountResponseDto(
        Long fanpageId,
        long followerCount,
        Boolean isFollowing
) {}
