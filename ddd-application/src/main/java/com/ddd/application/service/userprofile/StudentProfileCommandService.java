package com.ddd.application.service.studentprofile;

import com.ddd.application.dto.user.StudentProfileDto;

public interface StudentProfileCommandService {
    void createStudentProfile(StudentProfileDto studentProfileDto, Long userId);
}
