package com.ddd.api.dto.user.res;

import java.util.Set;

public record UserResponseDto(
        Long userId,
        String email,
        Set<String> roles,
        String role,
        Boolean isActive
) {
    public UserResponseDto( Long userId, String email, Set<String> roles, Boolean isActive) {
        this(userId, email, roles, (roles != null && !roles.isEmpty()) ? roles.iterator().next() : null, isActive);
    }

    public UserResponseDto(String email, String role, Boolean isActive) {
        this(null, email, role != null ? Set.of(role) : Set.of(), role, isActive);
    }
}
