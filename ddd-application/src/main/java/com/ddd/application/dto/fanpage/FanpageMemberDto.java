package com.ddd.application.dto.fanpage;

public record FanpageMemberDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String role
) {
}
