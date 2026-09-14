package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class User {
    private Long id;
    private String name;
    private int age;
    private String username;
    private String password;
    private String email;
    private String role;
}
