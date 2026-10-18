package com.ddd.application.service;

import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    LoginResult login(String email, String password);
    LoginResult loginWithGoogle(String idToken);
    ResponseCookie getUserCookie(String jwtToken);
    ResponseCookie getLogoutCookie();

    void createUser(UserDto userDto, StudentProfileDto studentProfileDto);

    void createFanpageAdmin(UserDto userDto, FanpageAdminProfileDto fanpageAdminProfileDto);
}

