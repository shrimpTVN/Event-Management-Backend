package com.ddd.application.service.user;

import com.ddd.application.dto.user.UserDto;

public interface UserCommandService {
    Long createUser(UserDto userDto);
}
