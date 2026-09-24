package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.auth.req.LoginRequestDto;
import com.ddd.api.dto.auth.req.RegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.api.mapper.AuthApiMapper;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthApiMapper authApiMapper;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        LoginResult loginResult = authService.login(loginRequestDto.email(), loginRequestDto.password());
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getUserCookie(loginResult.jwtToken()).toString())
                .body(BaseResponse.of(authApiMapper.toLoginResponse(loginResult.loginDto())));
    }

    @PostMapping("/register")
    public BaseResponse<Void> register(@RequestBody @Valid RegisterRequestDto userRegisterRequestDto) {
        UserDto userDto = authApiMapper.toUserDto(userRegisterRequestDto);
        StudentProfileDto studentProfileDto = authApiMapper.toUserProfileDto(userRegisterRequestDto);
        authService.createUser(userDto, studentProfileDto);
        return BaseResponse.ok();
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getLogoutCookie().toString())
                .body(BaseResponse.ok());
    }


    @PostMapping("/change-password")
    public BaseResponse<Void> changePassword() {
        System.out.println("change password");
        return BaseResponse.ok();
    }

}
