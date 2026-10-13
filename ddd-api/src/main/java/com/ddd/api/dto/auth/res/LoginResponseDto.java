package com.ddd.api.dto.auth.res;

import java.util.Set;

public record LoginResponseDto(
        Long userId,
        String email,
        Set<String> roles,
        String role
) {
    public LoginResponseDto(Long userId, String email, Set<String> roles) {
        this(userId, email, roles, (roles != null && !roles.isEmpty()) ? roles.iterator().next() : null);
    }

    public LoginResponseDto(Long userId, String email, String role) {
        this(userId, email, role != null ? Set.of(role) : Set.of(), role);
    }
}
