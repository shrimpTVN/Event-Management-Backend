package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Fanpage extends BaseModel {
    private String name;
    private String description;
    private String avatarUrl;
    private String status;
}
