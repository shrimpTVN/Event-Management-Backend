package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record FanpageAdminRegisterRequestDto(
        @NotNull @Size(min = 6) String email,
        @NotNull @Size(min = 6) String password,
        @NotNull @NotBlank String providerId,

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
