package com.ddd.application.dto.registration;

import com.ddd.application.dto.event.EventInfoDto;

import java.time.Instant;

public record RegistrationInfoDto(
        Long userId,
        Long eventId,
        Instant regDate,
        String status,
        String reason,
        Instant checkInAt,
        Instant checkOutAt,
        String checkInEvidenceUrl,
        String checkOutEvidenceUrl,
        Long checkInBy,
        Long checkOutBy,
        Instant createdAt,
        EventInfoDto eventInfo
) {
}
