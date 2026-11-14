package com.ddd.api.dto.fanpage.res;

public record ManagedFanpageResponseDto(
        Long fanpageId,
        String fanpageName,
        String fanpageDescription,
        String fanpageAvatarUrl,
        String fanpageOrgName,
        String role
) {
}
