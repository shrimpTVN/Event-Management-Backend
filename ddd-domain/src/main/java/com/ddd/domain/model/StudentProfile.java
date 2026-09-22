package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentProfile extends BaseModel {
    private String firstName;
    private String lastName;
    private String studentId;
    private String gender;
    private Integer kNumber;
    private String avatarUrl;
    private Long associationId;
    private Long majorId;
    private Long userId;
}
