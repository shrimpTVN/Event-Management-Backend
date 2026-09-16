package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.application.dto.auth.LoginDto;
import com.ddd.api.dto.auth.req.LoginRequestDto;
import com.ddd.api.dto.auth.req.UserRegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.api.mapper.auth.AuthMapper;
import com.ddd.application.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto){
        LoginDto loginDto = authService.login(loginRequestDto.username(), loginRequestDto.password());
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getUserCookie(loginDto.jwtToken()).toString())
                .body(BaseResponse.of(authMapper.toLoginResponse(loginDto)));
    }

    @PostMapping("/register")
    public BaseResponse<Void> register(@RequestBody UserRegisterRequestDto userRegisterRequestDto){
        authService.createUser(authMapper.toUserRegisterDto(userRegisterRequestDto));
        return BaseResponse.ok();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public BaseResponse<Void >changePassword(){
        System.out.println("change password");
        return BaseResponse.ok();
    }

}
