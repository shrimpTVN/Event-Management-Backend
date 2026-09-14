package com.ddd.application.service.auth.impl;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.UserRegisterDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.application.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserDtoMapper userDtoMapper;

    @Override
    public LoginDto login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            //refactor to Auth exception
            throw new IllegalArgumentException("Credentials don't match");
        }
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Credentials don't match");
        }

        return new LoginDto(user.getId(), "access_token", "user");
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
        user.setRole("USER");
        userRepository.save(user);

    }
}
