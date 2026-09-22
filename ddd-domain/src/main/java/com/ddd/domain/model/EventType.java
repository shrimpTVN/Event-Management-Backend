package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventType extends BaseModel {
    private String name;
    private String description;
}
