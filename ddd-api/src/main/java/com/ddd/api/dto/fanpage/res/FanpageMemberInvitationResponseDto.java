package com.ddd.api.dto.fanpage.res;

import java.time.Instant;

public record FanpageMemberInvitationResponseDto(
        Long id,
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl,
        Long userId,
        String userEmail,
        String userFirstName,
        String userLastName,
        String userAvatarUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
