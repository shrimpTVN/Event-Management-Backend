package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record EventUpdateRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,
        @NotNull @NotBlank String location,
        @NotNull LocalDate dateOpen,
        @NotNull LocalDate dateClose,
        @NotNull LocalDate dateHappen,
        @NotNull @Positive Integer capacity,
        @NotNull @Min(0) Integer maleQuantity,
        @NotNull @Min(0) Integer femaleQuantity,
        @NotNull @NotBlank String bannerUrl,
        @NotNull @Positive Long eventTypeId,
        @Positive Long criteriaId,
        @NotNull @Positive Long semesterId,
        @NotNull List<EventPointRequestDto> eventPoints
) {
}
