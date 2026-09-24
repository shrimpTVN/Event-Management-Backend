package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.*;

import java.io.Serializable;

public record RegisterRequestDto(@NotNull @Size(min = 6) String email,
                                 @NotNull @Size(min = 6) String password,
                                 @NotNull @NotBlank String providerId,
                                 @NotNull @NotBlank String firstName,
                                 @NotNull @NotBlank String lastName,
                                 @NotNull @NotBlank String studentId,
                                 @NotNull @NotBlank String gender,
                                 @NotNull @Positive @Max(100) Integer kNumber,
                                 @NotNull @Positive Integer associationId,
                                 @NotNull @Positive Integer majorId) implements Serializable {
}