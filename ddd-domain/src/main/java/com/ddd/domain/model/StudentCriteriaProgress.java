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
    private boolean completed;
    private CriteriaCompletionSourceEnum completionSource = CriteriaCompletionSourceEnum.MANUAL;
    private Long sourceEventId;
    private Instant completedAt;

    public StudentCriteriaProgress(Long userId, Long criteriaId, Long academicYearId) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
    }

    public void complete(CriteriaCompletionSourceEnum source, Long eventId, Instant completedAt) {
        this.completed = true;
        this.completionSource = source;
        this.sourceEventId = eventId;
        if (this.completedAt == null) {
            this.completedAt = completedAt;
        }
    }

    public void uncomplete() {
        this.completed = false;
        this.completionSource = CriteriaCompletionSourceEnum.MANUAL;
        this.sourceEventId = null;
        this.completedAt = null;
    }
}
