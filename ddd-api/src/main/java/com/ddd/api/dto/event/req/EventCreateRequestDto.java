package com.ddd.api.dto.event.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record EventCreateRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,
        @NotNull @NotBlank String location,
        @NotNull LocalDate dateOpen,
        @NotNull LocalDate dateClose,
        @NotNull LocalDate dateHappen,
        @NotNull @Positive Integer capacity,
        Integer maleQuantity,
        Integer femaleQuantity,
        @NotNull @NotBlank String bannerUrl,
        @NotNull @Positive Long eventTypeId,
        @Positive Long criteriaId,
        @NotNull @Positive Long fanpageId,
        @NotNull @Positive Long semesterId,
        @NotNull List<EventPointRequestDto> eventPoints
) {
}
