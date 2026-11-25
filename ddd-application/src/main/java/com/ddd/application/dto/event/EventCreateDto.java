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
        Boolean genderConstraint,
        String bannerUrl,
        Long eventTypeId,
        Long criteriaId,
        Long fanpageId,
        Long semesterId,
        Long schoolId,
        Boolean allowSelfCheck,
        List<EventPointCreateDto> eventPoints
) {
    public EventCreateDto(
            String name, String description, String location, Instant dateOpen, Instant dateClose,
            Instant dateHappen, Integer capacity, Integer maleQuantity, Integer femaleQuantity,
            String bannerUrl, Long eventTypeId, Long criteriaId, Long fanpageId, Long semesterId,
            Long schoolId, Boolean allowSelfCheck, List<EventPointCreateDto> eventPoints
    ) {
        this(name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                false, bannerUrl, eventTypeId, criteriaId, fanpageId, semesterId, schoolId, allowSelfCheck, eventPoints);
    }
}
