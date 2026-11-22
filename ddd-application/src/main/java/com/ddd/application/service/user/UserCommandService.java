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

    void createPassword(String newPassword);

    StudentProfileSummaryDto updateStudentProfile(String email, StudentProfileUpdateDto updateDto);

    StudentProfileSummaryDto updateStudentProfileById(Long id, StudentProfileUpdateDto updateDto);

    UserSummaryDto updateAvatar(String avatarUrl);

    default UserSummaryDto updateAvatar(String email, String avatarUrl) {
        return updateAvatar(avatarUrl);
    }

    UserSummaryDto updateProfileStatusById(Long id);

    void outFanpageMember(Long fanpageId);

    com.ddd.application.dto.user.StaffProfileSummaryDto updateStaffProfile(com.ddd.application.dto.user.StaffProfileUpdateDto updateDto);
}
