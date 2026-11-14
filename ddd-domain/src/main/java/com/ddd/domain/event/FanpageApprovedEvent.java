package com.ddd.domain.event;

public record FanpageApprovedEvent(
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        Long userId
) {
}

