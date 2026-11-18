package com.ddd.domain.event;

public record FanpageMemberAppointedEvent(
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        String appointerUserName,
        Long appointedUserId
) {
}
