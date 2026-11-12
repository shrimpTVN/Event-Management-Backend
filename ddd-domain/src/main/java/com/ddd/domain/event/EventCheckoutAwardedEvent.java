package com.ddd.domain.event;

import java.util.List;

public record EventCheckoutAwardedEvent(
        Long eventId,
        String eventName,
        String eventAvatarUrl,
        List<Long> userIds
) {
}
