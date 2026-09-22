package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PointCategory extends BaseModel {
    private String name;
    private String description;
    private Integer maximum;
    private LocalDate dateApply;
    private Long parentCategoryId;
}
