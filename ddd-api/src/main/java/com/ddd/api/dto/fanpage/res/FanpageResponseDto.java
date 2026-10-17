package com.ddd.api.dto.fanpage.res;

import java.time.Instant;

public record FanpageResponseDto(
        Long id,
        String name,
        String description,
        String avatarUrl,
        String orgType,
        String orgName,
        String parentOrg,
        String helpContact,
        String helpPhone,
        Instant createdAt,
        String status,
        Boolean isActive
) {
}
