package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AcademicYear extends BaseModel {
    private String code;
    private LocalDate startDate;
    private LocalDate endDate;
}
