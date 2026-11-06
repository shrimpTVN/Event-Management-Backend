package com.ddd.api.dto.registration.res;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.domain.enums.RegistrationStatusEnum;
import java.time.Instant;

public record RegistrationResponseDto(
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
