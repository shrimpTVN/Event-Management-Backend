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
    private Boolean isCompleted;
    private Instant completedAt;
    private Integer completedChildrenCount;

    public StudentCriteriaProgress(Long userId, Long criteriaId, Long academicYearId) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
        this.completedChildrenCount = 0;
    }


}
