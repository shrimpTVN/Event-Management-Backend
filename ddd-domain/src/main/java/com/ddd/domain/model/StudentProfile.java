package com.ddd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile extends BaseModel {

    private String studentId;
    private Integer kNumber;
    private Long majorId;
    private Long userId;


    public void assignUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        this.userId = userId;
    }

    public void assignStudentId(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID must not be blank");
        }
        this.studentId = studentId.trim();
    }
}
