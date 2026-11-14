package com.ddd.domain.event;

public record FanpageInvitationRejectedEvent(
        Long fanpageId,
        Long rejectedUserId,
        String rejectedUserName,
        String rejectedUserAvatarUrl
) {
}
