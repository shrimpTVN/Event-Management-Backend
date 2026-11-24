package com.ddd.application.dto.user;

import java.time.LocalDate;

public record StaffProfileDetailsCreateDto(
        String staffId,
        String orgName,
        String title,
        LocalDate dateOfBirth,
        String phoneNumber
) {
}
