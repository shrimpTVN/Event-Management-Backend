package com.ddd.application.dto.registration;

import com.ddd.domain.enums.RegistrationStatusEnum;

import java.time.Instant;

public record StudentRegistrationInfoDto(
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
        StudentProfileBasicInfoDto studentProfile
) {
}
