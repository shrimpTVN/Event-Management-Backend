package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class Semester extends BaseModel {
    private Integer number;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Boolean isCurrent;
}
