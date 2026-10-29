package com.ddd.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class PointTransaction {
    private Long id;
    private Long userId;
    private Long semesterId;
    private Long pointCategoryId;
    private Long eventId;
    private Integer points;
    private String type;
    private String status;
    private String description;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updateBy;
}
