package com.ddd.application.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record FanpageAdminProfileDto(
        @NotNull @NotBlank String firstName,
        @NotNull @NotBlank String lastName,
        @NotNull @NotBlank String staffId,
        @NotNull @NotBlank String orgName,
        @NotNull @NotBlank String title,
        @NotNull Instant DoB,
        @NotNull @NotBlank String gender,
        @NotNull @NotBlank String phoneNumber
) {
}
