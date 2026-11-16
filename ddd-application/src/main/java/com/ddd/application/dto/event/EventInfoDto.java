package com.ddd.application.dto.event;

import com.ddd.application.dto.SemesterDto;

import java.time.Instant;
import java.util.List;

public record EventInfoDto(
        Long id,
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
        String status,
        String eventTypeName,
        String criteriaName,
        String fanpageName,
        Long fanpageId,
        boolean isActive,
        boolean allowSelfCheck,
        SemesterDto semester,
        Instant updatedAt,
        List<EventPointInfoDto> eventPoints
) {
    public EventInfoDto(
            Long id,
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
            String status,
            String eventTypeName,
            String criteriaName,
            String fanpageName,
            Long fanpageId,
            boolean isActive,
            SemesterDto semester,
            Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeName, criteriaName, fanpageName, fanpageId, isActive, true, semester, updatedAt, eventPoints);
    }
}
