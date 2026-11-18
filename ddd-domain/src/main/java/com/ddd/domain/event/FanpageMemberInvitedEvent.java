package com.ddd.domain.event;

public record FanpageMemberInvitedEvent(
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        String inviterName,
        Long invitedUserId
) {
}
