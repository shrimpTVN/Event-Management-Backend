package com.ddd.application.service;

import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    LoginResult login(String email, String password);
    ResponseCookie getUserCookie(String jwtToken);
    ResponseCookie getLogoutCookie();

    void createUser(UserDto userDto, StudentProfileDto studentProfileDto);

    void createFanpageAdmin(UserDto userDto, FanpageAdminProfileDto fanpageAdminProfileDto);

    void changePassword(@NotNull @Size(min=6) String oldpPassword, @NotNull @Size(min=6) String newPassword, String email);
}
