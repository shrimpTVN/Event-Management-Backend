package com.ddd.domain.event;

public record FanpageInvitationAcceptedEvent(
        Long fanpageId,
        Long acceptedUserId,
        String acceptedUserName,
        String acceptedUserAvatarUrl
) {
}
