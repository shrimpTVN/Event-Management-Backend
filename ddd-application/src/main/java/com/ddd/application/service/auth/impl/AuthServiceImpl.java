package com.ddd.application.service.auth.impl;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.LoginResult;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.application.service.auth.AuthService;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

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

    @Override
    public void createUser(UserRegisterDto userRegisterRequest) {
        if (userRepository.existsByEmail(userRegisterRequest.email()))
        {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userDtoMapper.toUser(userRegisterRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
//        user.setRoleId(2L);
        userRepository.save(user);
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
