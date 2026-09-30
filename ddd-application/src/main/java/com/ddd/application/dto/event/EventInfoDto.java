package com.ddd.application.dto.event;

import java.time.Instant;
import java.time.LocalDate;

public record EventInfoDto(
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
        Instant updatedAt
) {
}
