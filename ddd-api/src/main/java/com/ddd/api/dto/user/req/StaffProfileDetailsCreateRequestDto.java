package com.ddd.api.dto.user.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record StaffProfileDetailsCreateRequestDto(
        @NotNull @NotBlank @Size(max = 20) String staffId,
        @NotNull @NotBlank @Size(max = 255) String orgName,
        @NotNull @NotBlank @Size(max = 100) String title,
        @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
        @Size(max = 20) String phoneNumber
) {
}
