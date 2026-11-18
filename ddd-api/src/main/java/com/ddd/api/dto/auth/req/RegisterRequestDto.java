package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record RegisterRequestDto(
        @NotNull @Size(min = 6) String email,
        @NotNull @Size(min = 6) String password,
        @NotNull @NotBlank String otpCode,
        @NotNull @NotBlank String firstName,
        @NotNull @NotBlank String lastName
) implements Serializable {
}
