package com.ddd.application.service.user;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileUpdateDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.dto.user.UserSummaryDto;

import java.util.Set;

public interface UserCommandService {
    Long createUser(UserDto userDto, String name);
    Long createUser(UserDto userDto, Set<String> roleNames);
    void assignRolesToUser(Long userId, Set<String> roleNames);

    void changePassword(String oldpPassword, String newPassword, String email);

    StudentProfileSummaryDto updateStudentProfile(String email, StudentProfileUpdateDto updateDto);

    StudentProfileSummaryDto updateStudentProfileById(Long id, StudentProfileUpdateDto updateDto);

    StudentProfileSummaryDto updateAvatar(String email, String avatarUrl);

    UserSummaryDto updateProfileStatusById(Long id);
}
