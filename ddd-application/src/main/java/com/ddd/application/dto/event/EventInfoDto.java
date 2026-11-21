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
        Long eventTypeId,
        String eventTypeName,
        Long criteriaId,
        String criteriaName,
        String fanpageName,
        Long fanpageId,
        boolean isActive,
        boolean allowSelfCheck,
        SemesterDto semester,
        Instant updatedAt,
        List<EventPointInfoDto> eventPoints,
        Long registeredCount
) {
    public EventInfoDto withRegisteredCount(Long count) {
        return new EventInfoDto(
                id, name, description, location, dateOpen, dateClose, dateHappen,
                capacity, maleQuantity, femaleQuantity, bannerUrl, status,
                eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName,
                fanpageId, isActive, allowSelfCheck, semester, updatedAt, eventPoints, count
        );
    }

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
            Long eventTypeId,
            String eventTypeName,
            Long criteriaId,
            String criteriaName,
            String fanpageName,
            Long fanpageId,
            boolean isActive,
            boolean allowSelfCheck,
            SemesterDto semester,
            Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId, isActive, allowSelfCheck, semester, updatedAt, eventPoints, 0L);
    }

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
            boolean allowSelfCheck,
            SemesterDto semester,
            Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId, isActive, allowSelfCheck, semester, updatedAt, eventPoints, 0L);
    }

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
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId, isActive, true, semester, updatedAt, eventPoints, 0L);
    }

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
            Long eventTypeId,
            String eventTypeName,
            Long criteriaId,
            String criteriaName,
            String fanpageName,
            Long fanpageId,
            boolean isActive,
            SemesterDto semester,
            Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId, isActive, true, semester, updatedAt, eventPoints, 0L);
    }
}
