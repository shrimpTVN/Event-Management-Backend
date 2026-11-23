package com.ddd.application.dto.user;

import java.util.Set;

public record UserInfoDto(
        Long userId,
        String email,
        Set<String> roles,
        String role,
        String firstName,
        String lastName,
        String gender,
        Boolean isActive,
        String providerId,
        String avatarUrl,
        StudentProfileDetailsDto studentProfile,
        StaffProfileDetailsDto staffProfile
) {
}
