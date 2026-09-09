package com.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDto(@NotNull Long userId, @NotNull String accessToken, @NotNull String role) {
}
