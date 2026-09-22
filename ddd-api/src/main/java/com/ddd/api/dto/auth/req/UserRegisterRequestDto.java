package com.ddd.api.dto.auth.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UserRegisterRequestDto(@NotBlank @Size(min = 6) String email,
                                     @NotBlank @Size(min = 6) String password) implements Serializable {
}