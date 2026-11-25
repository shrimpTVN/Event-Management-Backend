package com.ddd.api.dto.event.req;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record EventUpdateRequestDto(
        @NotNull @NotBlank String name,
        @NotNull @NotBlank String description,
        @NotNull @NotBlank String location,
        @NotNull Instant dateOpen,
        @NotNull Instant dateClose,
        @NotNull Instant dateHappen,
        @NotNull @Positive Integer capacity,
        @NotNull @Min(0) Integer maleQuantity,
        @NotNull @Min(0) Integer femaleQuantity,
        Boolean genderConstraint,
        @NotNull @NotBlank String bannerUrl,
        @NotNull @Positive Long eventTypeId,
        @Positive Long criteriaId,
        @NotNull @Positive Long semesterId,
        @Positive Long schoolId,
        Boolean allowSelfCheck,
        @NotNull List<EventPointRequestDto> eventPoints
) {
    public EventUpdateRequestDto {
        if (allowSelfCheck == null) {
            allowSelfCheck = true;
        }
        if (genderConstraint == null) {
            genderConstraint = false;
        }
    }
}
