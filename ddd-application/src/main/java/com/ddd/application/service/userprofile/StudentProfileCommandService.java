package com.ddd.application.service.userprofile;

import com.ddd.application.dto.user.StudentProfileDto;

public interface StudentProfileCommandService {
    void createStudentProfile(StudentProfileDto studentProfileDto, Long userId);
    void updateProfileByEmail(String email, StudentProfileDto studentProfileDto);
    void updateAvatarByEmail(String email, String avatarUrl);
    void updateProfileById(Long id, StudentProfileDto studentProfileDto);
    }
