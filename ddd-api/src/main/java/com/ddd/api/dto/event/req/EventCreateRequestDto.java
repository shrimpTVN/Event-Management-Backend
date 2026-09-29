package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventCreateRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,

) {
}
