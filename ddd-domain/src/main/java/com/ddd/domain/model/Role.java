package com.ddd.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, of = "name")
public class Role extends BaseModel {
    private String name;
    private String description;
}
