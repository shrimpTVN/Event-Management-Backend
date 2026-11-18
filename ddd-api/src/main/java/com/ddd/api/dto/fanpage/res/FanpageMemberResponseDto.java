package com.ddd.api.dto.fanpage.res;

public record FanpageMemberResponseDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String role,
        String staffId,
        String phoneNumber
) {
    public FanpageMemberResponseDto(Long userId, String email, String firstName, String lastName, String avatarUrl, String role) {
        this(userId, email, firstName, lastName, avatarUrl, role, null, null);
    }
}
