package com.ddd.application.dto.user;

public record StaffProfileUpdateDto(
        String firstName,
        String lastName,
        String gender,
        StaffProfileDetailsUpdateDto staffProfile
) {
}
