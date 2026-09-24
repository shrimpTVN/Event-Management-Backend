package com.ddd.domain.repository;

import com.ddd.domain.model.StudentProfile;
import jakarta.validation.constraints.NotBlank;

public interface StudentProfileRepository {
    boolean existsByStudentId(@NotBlank String studentId);

    void save(StudentProfile studentProfile, Long userId);
}
