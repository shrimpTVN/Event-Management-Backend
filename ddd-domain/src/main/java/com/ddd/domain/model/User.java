package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User extends BaseModel {

    private String email;
    private String password;
    private String provider;
    private Long roleId;

}
