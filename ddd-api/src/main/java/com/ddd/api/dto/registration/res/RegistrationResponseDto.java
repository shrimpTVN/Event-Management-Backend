package com.ddd.api.dto.registration.res;

import com.ddd.domain.enums.RegistrationStatusEnum;
import java.time.Instant;

public record RegistrationResponseDto(
        Long eventId,
        Instant regDate,
        RegistrationStatusEnum status,
        String reason,
        Instant checkInAt,
        Instant checkOutAt,
        String evidenceUrl,
        String eventName,
        String location,
        Instant dateOpen,
        Instant dateClose,
        Instant dateHappen,
        String bannerUrl
) {
}
