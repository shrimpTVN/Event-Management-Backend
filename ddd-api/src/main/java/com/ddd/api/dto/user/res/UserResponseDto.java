package com.ddd.api.dto.user.res;

import java.util.Set;

public record UserResponseDto(
        Long userId,
        String email,
        Set<String> roles,
        String role,
        String firstName,
        String lastName,
        String gender,
        Boolean isActive,
        String providerId,
        String avatarUrl
) {

}
