package com.ddd.application.service.impl;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.service.userprofile.FanpageAdminProfileCommandService;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.application.service.AuthService;
import com.ddd.domain.enums.RoleEnum;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserCommandService userCommandService;
    private final StudentProfileCommandService studentProfileCommandService;
    private final FanpageAdminProfileCommandService fanpageAdminProfileCommandService;
    private final JwtUtil jwtUtil;


    @Override
    public LoginResult login(String email, String password) {
        var resultAuthentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        String jwtToken = jwtUtil.generateJwtToken(resultAuthentication);

        var fetchedUser = (UserDetailsCustom) resultAuthentication.getPrincipal();

        if (fetchedUser == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User:{} logged in successfully", email);

        LoginDto loginDto = new LoginDto(
                fetchedUser.getUserId(),
                fetchedUser.getUsername(),
                fetchedUser.getRole()
        );

        return new LoginResult(loginDto, jwtToken);
    }

    @Transactional
    @Override
    public void createUser(UserDto userDto, StudentProfileDto studentProfileDto) {
        Long userId = userCommandService.createUser(userDto, RoleEnum.STUDENT.name());
        studentProfileCommandService.createStudentProfile(studentProfileDto, userId);
        log.info("Student:{} created successfully", userDto.email());
    }

    @Transactional
    @Override
    public void createFanpageAdmin(UserDto userDto, FanpageAdminProfileDto fanpageAdminProfileDto) {
        Long userId = userCommandService.createUser(userDto, RoleEnum.FANPAGE_ADMIN.name());
        fanpageAdminProfileCommandService.createFanpageAdminProfile(fanpageAdminProfileDto, userId);
        log.info("Fanpage Admin:{} created successfully", userDto.email());
    }

    @Override
    public ResponseCookie getUserCookie(String jwtToken) {
        return ResponseCookie.from(jwtUtil.getCookieName(), jwtToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

    @Override
    public ResponseCookie getLogoutCookie() {
        return ResponseCookie.from(jwtUtil.getCookieName(), "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }
}
