package com.ddd.application.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDto(@NotNull @Size(min = 6) String email,
                      @NotNull @Size(min = 6) String password,
                      @NotBlank String providerId) {
}
