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
        String evidenceUrl,
        String helpContact,
        String helpPhone,
        Instant createdAt,
        String status,
        Boolean isActive,
        Long followerCount,
        Long eventsCount,
        Long representativeUserId,
        String representativeName,
        String representativeEmail,
        String representativeCode,
        String representativePhone
) {
    public FanpageResponseDto(
            Long id,
            String name,
            String description,
            String avatarUrl,
            String orgType,
            String orgName,
            String parentOrg,
            String evidenceUrl,
            String helpContact,
            String helpPhone,
            Instant createdAt,
            String status,
            Boolean isActive
    ) {
        this(id, name, description, avatarUrl, orgType, orgName, parentOrg, evidenceUrl,
                helpContact, helpPhone, createdAt, status, isActive,
                null, null, null, null, null, null, null);
    }
}
