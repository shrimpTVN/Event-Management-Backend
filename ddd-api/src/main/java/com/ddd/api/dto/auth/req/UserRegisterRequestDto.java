package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UserRegisterRequestDto(@NotBlank @Size(max=100) String name,
                                     @NotNull int age,
                                     @NotBlank @Size(min=6) String username,
                                     @NotBlank @Size(min=6) String email,
                                     @NotBlank @Size(min=6) String password) implements Serializable {
}