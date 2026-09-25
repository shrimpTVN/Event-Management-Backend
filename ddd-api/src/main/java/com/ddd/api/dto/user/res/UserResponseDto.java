package com.ddd.api.dto.user.res;

public record UserResponseDto(
        String email,
        String role,
        Boolean isActive
) {
}
