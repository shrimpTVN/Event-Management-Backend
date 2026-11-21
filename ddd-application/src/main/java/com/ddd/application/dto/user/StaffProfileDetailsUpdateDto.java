package com.ddd.application.dto.user;

import java.time.LocalDate;

public record StaffProfileDetailsUpdateDto(
        String title,
        LocalDate dateOfBirth,
        String phoneNumber
) {
}
