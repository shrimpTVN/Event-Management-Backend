package com.ddd.api.dto.event.res;

import com.ddd.api.dto.semester.SemesterResponseDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record EventResponseDto(
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
        SemesterResponseDto semester,
        List<EventPointResponseDto> eventPoints,
        Instant updatedAt,
        Long registeredCount,
        @JsonProperty("isFollowFanpage") boolean isFollowFanpage,
        @JsonProperty("isFull") boolean isFull,
        boolean registrationClosed,
        boolean genderConstraint,
        String violationReason
) {
    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId,
                schoolId, schoolName, isActive, allowSelfCheck, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }

    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt
    ) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId,
                null, null, isActive, allowSelfCheck, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }

    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                schoolId, schoolName, isActive, true, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }

    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                null, null, isActive, allowSelfCheck, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }

    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, null, eventTypeName, null, criteriaName, fanpageName, fanpageId,
                null, null, isActive, true, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }

    public EventResponseDto(
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
            SemesterResponseDto semester,
            List<EventPointResponseDto> eventPoints,
            Instant updatedAt) {
        this(id, name, description, location, dateOpen, dateClose, dateHappen, capacity, maleQuantity, femaleQuantity,
                bannerUrl, status, eventTypeId, eventTypeName, criteriaId, criteriaName, fanpageName, fanpageId,
                null, null, isActive, true, semester, eventPoints, updatedAt, 0L, false, false, false, false, null);
    }
}
