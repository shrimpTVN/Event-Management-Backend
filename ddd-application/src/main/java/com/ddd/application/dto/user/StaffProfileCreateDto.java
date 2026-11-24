package com.ddd.application.dto.user;

public record StaffProfileCreateDto(
        String firstName,
        String lastName,
        String gender,
        StaffProfileDetailsCreateDto staffProfile
) {
}
