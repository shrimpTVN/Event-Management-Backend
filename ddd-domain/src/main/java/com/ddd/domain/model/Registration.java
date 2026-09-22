package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Registration {
    private Long userId;
    private Long eventId;
    private Instant regDate;
    private Instant checkInAt;
    private Instant checkOutAt;
    private String evidenceUrl;
    private String status;
    private String reason;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
