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
    private String firstName;
    private String lastName;
    private String studentId;
    private String gender;
    private Integer kNumber;
    private String avatarUrl;

    private Long associationId;
    private Long majorId;
    private Long userId;

    public void updateInfo(String firstName, String lastName, String gender, Integer kNumber,
                           String avatarUrl, Long associationId, Long majorId) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (kNumber != null) {
            this.kNumber = kNumber;
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
        this.associationId = associationId;
        this.majorId = majorId;
    }

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
