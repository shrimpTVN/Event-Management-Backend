package com.ddd.api.dto.user.res;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record UserInfoResponseDto(
        Long userId,
        String email,
        Set<String> roles,
        String role,
        String firstName,
        String lastName,
        String gender,
        Boolean isActive,
        String providerId,
        String avatarUrl,
        StudentProfileDetailsResponseDto studentProfile,
        StaffProfileDetailsResponseDto staffProfile
) {
    @JsonProperty("fanpageAdminProfile")
    public StaffProfileDetailsResponseDto fanpageAdminProfile() {
        return staffProfile;
    }
}
