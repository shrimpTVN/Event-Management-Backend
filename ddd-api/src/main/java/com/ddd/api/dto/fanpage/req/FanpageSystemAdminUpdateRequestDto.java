package com.ddd.api.dto.fanpage.req;

public record FanpageSystemAdminUpdateRequestDto(
        String name,
        String description,
        String avatarUrl,
        String orgType,
        String orgName,
        String parentOrg,
        String helpContact,
        String helpPhone,
        String status,
        Boolean isActive
) {
}
