package com.ddd.application.dto.registration;

import com.ddd.application.dto.event.EventPointInfoDto;

import java.time.Instant;
import java.util.Set;

public record RegistrationDto(
        // 1. Registration info
        Long eventId,
        Instant regDate,
        String status,
        String reason,
        Instant checkInAt,
        Instant checkOutAt,
        String evidenceUrl,

        // 2. Event info
        String eventName,
        String location,
        Instant dateOpen,
        Instant dateClose,
        Instant dateHappen,
        String bannerUrl,

        //3. Benefits info
        Set<EventPointInfoDto> eventPointInfoDtos,
        Long criteriaId,
        String criteriaName,

        //4. Fanpage info
        Long fanpageId,
        String fanpageName,
        String fanpageAvatarUrl
) {
}
