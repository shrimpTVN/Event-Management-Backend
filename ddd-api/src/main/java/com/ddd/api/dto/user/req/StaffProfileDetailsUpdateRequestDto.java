package com.ddd.api.dto.user.req;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StaffProfileDetailsUpdateRequestDto(
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phoneNumber
) {
}
