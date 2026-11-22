package com.ddd.application.dto.fanpage;

public record FanpageMemberDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        String role,
        String staffId,
        String phoneNumber
) {
    public FanpageMemberDto(Long userId, String email, String firstName, String lastName, String avatarUrl, String role) {
        this(userId, email, firstName, lastName, avatarUrl, role, null, null);
    }
}
