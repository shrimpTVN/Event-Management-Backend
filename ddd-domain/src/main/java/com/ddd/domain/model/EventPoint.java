package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventPoint {
    private Long eventId;
    private Long pointCategoryId;
    private Integer point;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
    private Long updatedBy;

}
