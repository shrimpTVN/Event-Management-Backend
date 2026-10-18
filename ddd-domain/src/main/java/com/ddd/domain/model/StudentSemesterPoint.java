package com.ddd.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class StudentSemesterPoint {
    private Long userId;
    private Long semesterId;
    private Long pointCategoryId;
    private Integer totalPoint;
    private Instant finalizedAt;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public StudentSemesterPoint(Long userId, Long semesterId, Long pointCategoryId, Integer totalPoint) {
        this.userId = userId;
        this.semesterId = semesterId;
        this.pointCategoryId = pointCategoryId;
        this.totalPoint = totalPoint;

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.createdBy = null;
        this.updatedBy = null;
    }
}
