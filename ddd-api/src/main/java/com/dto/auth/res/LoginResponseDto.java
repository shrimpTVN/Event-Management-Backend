package com.dto.auth.res;

import jakarta.validation.constraints.NotNull;

public record LoginResponseDto(@NotNull Long userId, @NotNull String accessToken, @NotNull String role) {
}
