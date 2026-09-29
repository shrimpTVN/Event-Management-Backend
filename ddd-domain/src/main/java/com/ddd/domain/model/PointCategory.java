package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PointCategory extends BaseModel {
    private String name;
    private String note="";
    private Integer maximum;
    private LocalDate dateApply;
    private Integer level;
    private Long parentCategoryId;
    private Integer childrenOrder = 1;

    public void setLevel(Integer level) {
        if (level < 0) {
            throw new IllegalArgumentException("Level cannot be negative");
        }

        this.level = level;
    }

    public void setChildrenOrder(Integer childrenOrder) {
        if (childrenOrder == null || childrenOrder < 0) {
            throw new IllegalArgumentException("Children order must not be null and must be non-negative");
        }

        this.childrenOrder = childrenOrder;
    }
}
