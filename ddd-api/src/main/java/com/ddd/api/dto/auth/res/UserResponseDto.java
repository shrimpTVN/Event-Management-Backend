package com.ddd.api.dto.auth.res;

import com.ddd.infrastructure.entity.UserJpaEntity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for {@link UserJpaEntity}
 */
public record UserResponseDto(Long id,
                              @NotBlank String name,
                              @Min(1) int age,
                              @Size(min = 6) @NotBlank String username,
                              @Size(min = 6) @NotBlank String email) implements Serializable {
}