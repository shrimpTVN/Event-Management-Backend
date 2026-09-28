package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FanpageAdminProfile extends BaseModel {
    private String firstName;
    private String lastName;
    private String staffId;
    private String orgName;
    private String title;
    private Instant DoB;
    private String gender;
    private String phoneNumber;
    private String avatarUrl;
    private Long userId;

}
