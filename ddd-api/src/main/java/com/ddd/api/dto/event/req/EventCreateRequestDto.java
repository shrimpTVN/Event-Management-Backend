package com.ddd.api.dto.event.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record EventCreateRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,
        @NotNull @NotBlank String location,
        @NotNull Instant dateOpen,
        @NotNull Instant dateClose,
        @NotNull Instant dateHappen,
        @NotNull @Positive Integer capacity,
        @NotNull @Min(0) Integer maleQuantity,
        @NotNull @Min(0) Integer femaleQuantity,
        @NotNull @NotBlank String bannerUrl,
        @NotNull @Positive Long eventTypeId,
        @Positive Long criteriaId,
        @NotNull @Positive Long fanpageId,
        @NotNull @Positive Long semesterId,
        List<@Valid EventPointRequestDto> eventPoints
) {
}
