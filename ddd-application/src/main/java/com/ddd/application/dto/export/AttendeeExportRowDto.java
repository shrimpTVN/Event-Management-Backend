package com.ddd.application.dto.export;

import java.time.Instant;

public record AttendeeExportRowDto(
        int index,
        String studentId,
        String firstName,
        String lastName,
        String email,
        String gender,
        String majorName,
        Instant regDate,
        String status,
        Instant checkInAt,
        Instant checkOutAt,
        String reason
) {
}
