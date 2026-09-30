package com.ddd.api.dto.event.res;

import com.ddd.api.dto.semester.SemesterResponseDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EventResponseDto(
                Long id,
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
                String status,
                String eventTypeName,
                String criteriaName,
                String fanpageName,
                Long fanpageId,
                boolean isActive,
                SemesterResponseDto semester,
                List<EventPointResponseDto> eventPoints,
                Instant updatedAt) {
}
