package com.ddd.domain.event;

import java.util.List;

public record EventCreatedEvent(
        String fanpageName,
        String eventName,
        String eventAvatarUrl,
        Long eventId,
        List<Long>followerUserIds
) {
}

