package com.ddd.domain.model;

import com.ddd.domain.enums.SemesterStatusEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Semester extends BaseModel {
    private Integer number;
    private Long academicYearId;
    private LocalDate startDate;
    private LocalDate endDate;
    private SemesterStatusEnum status;
}
