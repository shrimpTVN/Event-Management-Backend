package com.ddd.application.service.auth.impl;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.application.service.auth.AuthService;
import com.ddd.infrastructure.config.security.custom.UserDetailsCustom;
import com.ddd.infrastructure.constant.ApplicationConstants;
import com.ddd.infrastructure.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public LoginDto login(String username, String password) {
       var resultAuthentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
       String jwtToken = jwtUtil.generateJwtToken(resultAuthentication);

       var fetchedUser = (UserDetailsCustom) resultAuthentication.getPrincipal();
       User user = new User();

       if (fetchedUser != null){
           user = userRepository.findByUsername(fetchedUser.getUsername());
       } else {
           throw new BadCredentialsException("Invalid username or password");
       }

        log.info("User:{} logged in successfully", username);
        return new LoginDto(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                jwtToken
        );
    }

    @Override
    public void createUser(UserRegisterDto userRegisterRequest) {
        if (userRepository.existsByUsername(userRegisterRequest.username()))
        {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(userRegisterRequest.email()))
        {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userDtoMapper.toUser(userRegisterRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);

    }

    public ResponseCookie getUserCookie(String jwtToken) {
        return ResponseCookie.from(jwtUtil.getCookieName(), jwtToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(jwtUtil.getExpirationMs() / 1000)
                .build();
    }


}
