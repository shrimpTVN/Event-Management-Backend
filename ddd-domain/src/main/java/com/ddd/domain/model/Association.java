package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Association extends BaseModel {
    private String name;
    private String address;
    private String description;
}
