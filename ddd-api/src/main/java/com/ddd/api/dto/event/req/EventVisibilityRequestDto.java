package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.NotNull;

public record EventVisibilityRequestDto(
        @NotNull Boolean hidden,
        String reason
) {
}
