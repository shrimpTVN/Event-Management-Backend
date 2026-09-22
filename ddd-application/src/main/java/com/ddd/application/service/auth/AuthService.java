package com.ddd.application.service.auth;

import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.UserRegisterDto;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    LoginResult login(String email, String password);
    void createUser(UserRegisterDto userRegisterRequest);
    ResponseCookie getUserCookie(String jwtToken);
    ResponseCookie getLogoutCookie();
}
