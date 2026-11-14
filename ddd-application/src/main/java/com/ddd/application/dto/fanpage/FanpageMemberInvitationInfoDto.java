package com.ddd.application.dto.fanpage;

import java.time.Instant;

public record FanpageMemberInvitationInfoDto(
        Long id,
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        Long userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        String userAvatarUrl,
        String inviterName,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
