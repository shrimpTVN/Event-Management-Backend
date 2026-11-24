package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ViewedEventsRequestDto(
        @NotNull UUID batchId,
        @NotNull @Size(max = 1000) List<@NotNull @Positive Long> eventIds
) {}
