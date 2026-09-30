package com.ddd.application.dto.event;

import java.time.LocalDate;
import java.util.List;

public record EventCreateDto(
        String name,
        String description,
        String location,
        LocalDate dateOpen,
        LocalDate dateClose,
        LocalDate dateHappen,
        Integer capacity,
        Integer maleQuantity,
        Integer femaleQuantity,
        String bannerUrl,
        Long eventTypeId,
        Long criteriaId,
        Long fanpageId,
        List<EventPointCreateDto> eventPoints
) {
}
