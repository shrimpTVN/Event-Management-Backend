package com.ddd.domain.event;

public record FanpageRejectedEvent(
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        Long userId
) {
}
