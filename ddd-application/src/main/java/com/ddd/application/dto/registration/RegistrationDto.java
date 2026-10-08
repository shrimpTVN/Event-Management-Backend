package com.ddd.application.dto.registration;

import java.time.Instant;

public record RegistrationDto(
        // 1. Thông tin Đăng ký (Từ RegistrationJpaEntity)
        Long eventId,
        Instant regDate,
        String status,
        String reason,
        Instant checkInAt,
        Instant checkOutAt,
        String evidenceUrl,

        // 2. Thông tin Sự kiện đi kèm (Từ EventJpaEntity)
        String eventName,
        String location,
        Instant dateOpen,
        Instant dateClose,
        Instant dateHappen,
        String bannerUrl
) {
}
