package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Major extends BaseModel {
    private String code;
    private String name;
    private Long schoolId;
}
