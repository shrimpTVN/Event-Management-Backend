package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class StudentSemesterPoint {
    private Long userId;
    private Long semesterId;
    private Long pointCategoryId;
    private BigDecimal totalPoint;
    private Instant finalizedAt;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;
}
