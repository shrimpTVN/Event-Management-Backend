package com.ddd.application.dto.user;

import java.util.Set;

public record StaffProfileSummaryDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String gender,
        String avatarUrl,
        Set<String> roles,
        StaffProfileDetailsDto staffProfile
) {
}
