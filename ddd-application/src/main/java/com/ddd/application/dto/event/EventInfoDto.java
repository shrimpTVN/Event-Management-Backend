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
        Long schoolId,
        String schoolName,
        boolean isActive,
        boolean allowSelfCheck,
        SemesterDto semester,
        Instant updatedAt,
        List<EventPointInfoDto> eventPoints,
        Long registeredCount,
        boolean isFollowFanpage,
        boolean isFull,
        boolean registrationClosed,
        boolean genderConstraint
) {
    public EventInfoDto withRegistrationInfo(long count, boolean followsFanpage, boolean full, boolean closed) {
        return new EventInfoDto(
                id, name, description, location, dateOpen, dateClose, dateHappen,
                capacity, maleQuantity, femaleQuantity, bannerUrl, status,
                eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName,
                fanpageId, schoolId, schoolName, isActive, allowSelfCheck, semester, updatedAt, eventPoints,
                count, followsFanpage, full, closed, genderConstraint
        );
    }

    public EventInfoDto(
            Long id, String name, String description, String location, Instant dateOpen, Instant dateClose,
            Instant dateHappen, Integer capacity, Integer maleQuantity, Integer femaleQuantity,
            String bannerUrl, String status, Long eventTypeId, String eventTypeName, Long criteriaId,
            String criteriaName, String fanpageName, Long fanpageId, Long schoolId, String schoolName,
            boolean isActive, boolean allowSelfCheck, SemesterDto semester, Instant updatedAt,
            List<EventPointInfoDto> eventPoints, Long registeredCount
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity,
                femaleQuantity, bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName,
                fanpageName, fanpageId, schoolId, schoolName, isActive, allowSelfCheck, semester, updatedAt,
                eventPoints, registeredCount, false, false, false, false);
    }

    public EventInfoDto(
            Long id, String name, String description, String location, Instant dateOpen, Instant dateClose,
            Instant dateHappen, Integer capacity, Integer maleQuantity, Integer femaleQuantity,
            String bannerUrl, String status, Long eventTypeId, String eventTypeName, Long criteriaId,
            String criteriaName, String fanpageName, Long fanpageId, Long schoolId, String schoolName,
            boolean isActive, boolean allowSelfCheck, SemesterDto semester, Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity,
                femaleQuantity, bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName,
                fanpageName, fanpageId, schoolId, schoolName, isActive, allowSelfCheck, semester, updatedAt,
                eventPoints, 0L, false, false, false, false);
    }

    public EventInfoDto(
            Long id, String name, String description, String location, Instant dateOpen, Instant dateClose,
            Instant dateHappen, Integer capacity, Integer maleQuantity, Integer femaleQuantity,
            String bannerUrl, String status, Long eventTypeId, String eventTypeName, Long criteriaId,
            String criteriaName, String fanpageName, Long fanpageId, boolean isActive, boolean allowSelfCheck,
            SemesterDto semester, Instant updatedAt, List<EventPointInfoDto> eventPoints, Long registeredCount
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity,
                femaleQuantity, bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName,
                fanpageName, fanpageId, null, null, isActive, allowSelfCheck, semester, updatedAt,
                eventPoints, registeredCount, false, false, false, false);
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
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId,
                null, null, isActive, allowSelfCheck, semester, updatedAt, eventPoints, 0L);
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
            Long schoolId,
            String schoolName,
            boolean isActive,
            SemesterDto semester,
            Instant updatedAt,
            List<EventPointInfoDto> eventPoints
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                schoolId, schoolName, isActive, true, semester, updatedAt, eventPoints, 0L);
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
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                null, null, isActive, allowSelfCheck, semester, updatedAt, eventPoints, 0L);
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
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                null, null, isActive, true, semester, updatedAt, eventPoints, 0L);
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
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId,
                null, null, isActive, true, semester, updatedAt, eventPoints, 0L);
    }
}
