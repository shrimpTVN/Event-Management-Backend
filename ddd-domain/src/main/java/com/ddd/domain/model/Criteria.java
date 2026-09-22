package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Criteria extends BaseModel {
    private String name;
    private String description;
    private String scope;
}
