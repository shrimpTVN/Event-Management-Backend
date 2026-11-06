package com.ddd.application.dto.event;

import java.time.Instant;
import java.util.List;

public record EventCreateDto(
        String name,
        String description,
        String location,
        Instant dateOpen,
        Instant dateClose,
        Instant dateHappen,
        Integer capacity,
        Integer maleQuantity,
        Integer femaleQuantity,
        String bannerUrl,
        Long eventTypeId,
        Long criteriaId,
        Long fanpageId,
        Long semesterId,
        List<EventPointCreateDto> eventPoints
) {
}
