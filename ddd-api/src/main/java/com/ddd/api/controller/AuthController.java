package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.application.dto.auth.LoginDto;
import com.ddd.api.dto.auth.req.LoginRequestDto;
import com.ddd.api.dto.auth.req.UserRegisterRequestDto;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.api.mapper.auth.AuthMapper;
import com.ddd.application.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthMapper authMapper;

    @GetMapping
    public BaseResponse<String> greeting(){
        return BaseResponse.of("This is from AuthController");
    }

    @PostMapping("/login")
    public BaseResponse<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto){
        LoginDto loginDto = authService.login(loginRequestDto.username(), loginRequestDto.password());
        return BaseResponse.of(authMapper.toLoginResponse(loginDto));
    }

    @PostMapping("/register")
    public BaseResponse<Void> register(@RequestBody UserRegisterRequestDto userRegisterRequestDto){
        authService.createUser(authMapper.toUserRegisterDto(userRegisterRequestDto));
        return BaseResponse.ok();
    }

}
