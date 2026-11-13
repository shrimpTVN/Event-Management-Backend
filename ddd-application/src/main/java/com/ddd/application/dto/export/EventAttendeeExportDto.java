package com.ddd.application.dto.export;

import java.time.Instant;
import java.util.List;

public record EventAttendeeExportDto(
        Long eventId,
        String eventName,
        String location,
        Instant dateHappen,
        Instant dateOpen,
        Instant dateClose,
        Integer capacity,
        String status,
        List<AttendeeExportRowDto> attendees
) {
}
