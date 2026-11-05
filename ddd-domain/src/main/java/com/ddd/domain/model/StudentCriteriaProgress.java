package com.ddd.domain.model;

import com.ddd.domain.enums.CriteriaCompletionSourceEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class StudentCriteriaProgress extends BaseModel {
    private Long userId;
    private Long criteriaId;
    private Long academicYearId;
    private Boolean isCompleted = Boolean.FALSE;
    private Instant completedAt;
    private Integer completedChildrenCount = 0;

    public StudentCriteriaProgress(Long userId, Long criteriaId, Long academicYearId) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
        this.completedChildrenCount = 0;
        this.isCompleted = Boolean.FALSE;
    }

    public void setIsCompleted(Boolean isCompleted) {
        this.isCompleted = isCompleted != null ? isCompleted : Boolean.FALSE;
    }
}
