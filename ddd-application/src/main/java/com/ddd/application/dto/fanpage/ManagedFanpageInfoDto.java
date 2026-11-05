package com.ddd.application.dto.fanpage;

public record ManagedFanpageInfoDto(
        Long fanpageId,
        String fanpageName,
        String fanpageDescription,
        String fanpageAvatarUrl,
        String fanpageOrgName,
        String role) {
}
