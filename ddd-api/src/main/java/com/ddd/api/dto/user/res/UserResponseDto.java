package com.ddd.api.dto.user.res;

public record UserResponseDto(
        Long id,
        String email,
        String role,
        Boolean isActive
) {
}
