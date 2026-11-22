package com.ddd.api.dto.user.res;

import java.util.Set;

public record StaffProfileResponseDto(
        Long userId,
        String firstName,
        String lastName,
        String email,
        String gender,
        String avatarUrl,
        Set<String> roles,
        StaffProfileDetailsResponseDto staffProfile
) {
}
