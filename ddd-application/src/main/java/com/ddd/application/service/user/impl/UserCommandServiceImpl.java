package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.UserDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserDtoMapper userDtoMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long createUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.email())) {
            throw new IllegalArgumentException("User with email " + userDto.email() + " already exists.");
        }

        var user = userDtoMapper.toUser(userDto);
        user.setPassword(passwordEncoder.encode(userDto.password()));
        System.out.println("Creating user: " + user.getEmail() + ", encoded password: " + user.getPassword());
        User savedUser = userRepository.save(user);
        return savedUser.getId();
    }
}
