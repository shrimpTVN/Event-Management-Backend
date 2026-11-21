package com.ddd.api.dto.registration.res;

import com.ddd.domain.enums.RegistrationStatusEnum;

import java.time.Instant;

public record StudentRegistrationResponseDto(
        Long userId,
        Long eventId,
        RegistrationStatusEnum status,
        Instant regDate,
        Instant checkInAt,
        String checkInEvidenceUrl,
        String checkInByName,
        Instant checkOutAt,
        String checkOutEvidenceUrl,
        String checkOutByName,
        String reason,
        Instant createdAt,
        StudentProfileBasicResponseDto studentProfile
) {
}
