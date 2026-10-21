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
    private Long eventId;
    private String note="";
    private CriteriaCompletionSourceEnum completionSource = CriteriaCompletionSourceEnum.MANUAL;


    public StudentCriteriaProgress(Long userId, Long criteriaId, Long academicYearId, String note) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
        this.note = note;
    }

    public StudentCriteriaProgress(Long userId, Long criteriaId, Long academicYearId, String note, Long eventId) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
        this.eventId = eventId;
        this.note = note;
        completionSource = CriteriaCompletionSourceEnum.EVENT_CHECKOUT;
    }

}
