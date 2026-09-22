package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role extends BaseModel {
    private String name;
    private String description;
}
