package com.controller;

import com.dto.auth.LoginDto;
import com.dto.auth.req.LoginRequestDto;
import com.dto.auth.res.LoginResponseDto;
import com.service.auth.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/login")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @GetMapping
    public String login(){
        return "Trying to login";
    }

    @PostMapping
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto){
        LoginDto loginDto = loginService.login(loginRequestDto.username(), loginRequestDto.password());
        return ResponseEntity.ok(new LoginResponseDto(loginDto.userId(),loginDto.accessToken(), loginDto.role()));
    }
}
