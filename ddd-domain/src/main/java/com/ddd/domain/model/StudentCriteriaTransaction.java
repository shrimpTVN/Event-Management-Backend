package com.ddd.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentCriteriaTransaction extends BaseModel{
    private Long userId;
    private Long criteriaId;
    private Long academicYearId;
    private Long eventId;
    private String completionSource;
    private String note;

    public StudentCriteriaTransaction(Long userId, Long criteriaId, Long academicYearId, Long eventId,
                                      String completionSource, String note) {
        this.userId = userId;
        this.criteriaId = criteriaId;
        this.academicYearId = academicYearId;
        this.eventId = eventId;
        this.completionSource = completionSource;
        this.note = note;
    }


}
