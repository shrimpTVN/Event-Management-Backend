package com.ddd.application.service.user;

import com.ddd.application.dto.user.UserDto;

import java.util.Set;

public interface UserCommandService {
    Long createUser(UserDto userDto, String name);
    Long createUser(UserDto userDto, Set<String> roleNames);
    void assignRolesToUser(Long userId, Set<String> roleNames);

    void changePassword(String oldpPassword, String newPassword, String email);
}
