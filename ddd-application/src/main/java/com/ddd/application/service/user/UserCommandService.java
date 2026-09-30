package com.ddd.application.service.user;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;

public interface UserCommandService {
    Long createUser(UserDto userDto, String name);
    void changeStatusUser(Long id);
    void changeRoleUser(Long id, String roleName);
}
