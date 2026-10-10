package com.ddd.api.dto.fanpage.res;

public record FanpageMemberResponseDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String role
) {
}
