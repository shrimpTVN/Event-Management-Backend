package com.ddd.application.dto.fanpage;

public record FanpageSystemAdminUpdateDto(
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
