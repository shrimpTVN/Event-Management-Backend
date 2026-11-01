package com.ddd.application.service.userprofile;

import com.ddd.application.dto.user.StudentProfileDto;

public interface StudentProfileCommandService {
    void createStudentProfile(StudentProfileDto studentProfileDto, Long userId);
    void createGoogleStudentProfile(String firstName, String lastName, String avatarUrl, Long userId);
}
