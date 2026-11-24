package com.ddd.application.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDto(
        @NotNull @Size(min = 6) String email,
        String password,
        String providerId,
        String firstName,
        String lastName,
        String gender,
        String avatarUrl
) {
}
