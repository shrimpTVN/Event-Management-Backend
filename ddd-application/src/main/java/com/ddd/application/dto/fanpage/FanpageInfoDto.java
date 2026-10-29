package com.ddd.application.dto.fanpage;

import java.time.Instant;

public record FanpageInfoDto(
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
