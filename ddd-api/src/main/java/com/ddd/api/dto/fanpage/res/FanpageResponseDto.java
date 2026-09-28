package com.ddd.api.dto.fanpage.res;

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
        String createdAt,
        String status,
        Boolean isActive
) {
}
