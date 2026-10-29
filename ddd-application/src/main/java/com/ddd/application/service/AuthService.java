package com.ddd.application.service;

import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.TokenRefreshResultDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseCookie;

public interface AuthService {
    LoginResult login(String email, String password);
    LoginResult loginWithGoogle(String idToken);
    ResponseCookie getUserCookie(String jwtToken);
    ResponseCookie getLogoutCookie();
    ResponseCookie getRefreshCookie(String rawRefreshToken);
    ResponseCookie getClearRefreshCookie();
    String extractRefreshToken(HttpServletRequest request);

    void createUser(UserDto userDto, StudentProfileDto studentProfileDto);

    void createFanpageAdmin(UserDto userDto, FanpageAdminProfileDto fanpageAdminProfileDto);

    void changePassword(@NotNull @Size(min=6) String oldPassword, @NotNull @Size(min=6) String newPassword, String email);

    TokenRefreshResultDto refreshToken(String rawRefreshToken);
    void logout(String rawRefreshToken);
}

