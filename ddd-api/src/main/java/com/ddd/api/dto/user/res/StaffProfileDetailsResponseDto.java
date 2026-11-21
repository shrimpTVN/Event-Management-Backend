package com.ddd.api.dto.user.res;

import java.time.LocalDate;

public record StaffProfileDetailsResponseDto(
        String staffId,
        String orgName,
        String title,
        LocalDate dateOfBirth,
        String phoneNumber
) {
}
