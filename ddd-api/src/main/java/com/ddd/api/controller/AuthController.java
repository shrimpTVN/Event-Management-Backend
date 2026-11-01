package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.auth.req.*;
import com.ddd.api.dto.auth.res.LoginResponseDto;
import com.ddd.api.mapper.AuthApiMapper;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.TokenRefreshResultDto;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.AuthService;
import com.ddd.application.service.otp.OtpCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final OtpCommandService otpCommandService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        LoginResult loginResult = authService.login(loginRequestDto.email(), loginRequestDto.password());
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getUserCookie(loginResult.jwtToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authService.getRefreshCookie(loginResult.rawRefreshToken()).toString())
                .body(BaseResponse.of(authApiMapper.toLoginResponse(loginResult.loginDto())));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<BaseResponse<Void>> refreshToken(HttpServletRequest request) {
        String rawRefresh = authService.extractRefreshToken(request);
        TokenRefreshResultDto result = authService.refreshToken(rawRefresh);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getUserCookie(result.newAccessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authService.getRefreshCookie(result.newRawRefreshToken()).toString())
                .body(BaseResponse.ok());
    }

    @PostMapping("/register")
    public BaseResponse<Void> register(@RequestBody @Valid RegisterRequestDto userRegisterRequestDto) {
        UserDto userDto = authApiMapper.toUserDto(userRegisterRequestDto);
        StudentProfileDto studentProfileDto = authApiMapper.toUserProfileDto(userRegisterRequestDto);
        authService.createUser(userDto, studentProfileDto);
        return BaseResponse.ok();
    }

    @PostMapping("/register/fanpage-admin")
    public BaseResponse<Void> registerFanpageAdmin(@RequestBody @Valid FanpageAdminRegisterRequestDto fanpageAdminRegisterRequestDto) {
        UserDto userDto = authApiMapper.toUserDto(fanpageAdminRegisterRequestDto);
        FanpageAdminProfileDto fanpageAdminProfileDto = authApiMapper.toFanpageAdminProfileDto(fanpageAdminRegisterRequestDto);
        authService.createFanpageAdmin(userDto, fanpageAdminProfileDto);
        return BaseResponse.ok();
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(HttpServletRequest request) {
        String rawRefresh = authService.extractRefreshToken(request);
        authService.logout(rawRefresh);
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getLogoutCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authService.getClearRefreshCookie().toString())
                .body(BaseResponse.ok());
    }


    @PostMapping("/send-otp")
    public BaseResponse<Void> sendOtp(@RequestBody @Valid SendOtpRequestDto requestDto) {
        otpCommandService.sendOtp(requestDto.email());
        return BaseResponse.ok();
    }

    @PostMapping("/verify-otp")
    public BaseResponse<Void> verifyOtp(@RequestBody @Valid VerifyOtpRequestDto requestDto) {
        otpCommandService.verifyOtp(requestDto.email(), requestDto.otpCode());
        return BaseResponse.ok();
    }

    @PostMapping("/change-password")
    public BaseResponse<Void> changePassword(@RequestBody ChangePasswordRequestDto changePasswordRequestDto,
                                             Authentication authentication) {
        String email = authentication.getName();
        authService.changePassword(changePasswordRequestDto.oldPassword(), changePasswordRequestDto.newPassword(), email);

        return BaseResponse.ok();
    }

    @PostMapping("/login/google")
    public ResponseEntity<BaseResponse<LoginResponseDto>> loginWithGoogle(
            @RequestBody @Valid GoogleLoginRequestDto requestDto) {
        LoginResult loginResult = authService.loginWithGoogle(requestDto.idToken());
        return ResponseEntity
                .status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, authService.getUserCookie(loginResult.jwtToken()).toString())
                .header(HttpHeaders.SET_COOKIE, authService.getRefreshCookie(loginResult.rawRefreshToken()).toString())
                .body(BaseResponse.of(authApiMapper.toLoginResponse(loginResult.loginDto())));
    }
}
