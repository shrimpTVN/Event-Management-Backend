package com.ddd.application.dto.semesterpoint;

import java.time.Instant;

public record PointTransactionInfoDto(Long id,
                                      Long pointCategoryId,
                                      Long eventId,
                                      Long semesterId,
                                      String eventName,
                                      Integer points,
                                      String type,
                                      String status,
                                      String description,
                                      Instant createdAt) {
}
