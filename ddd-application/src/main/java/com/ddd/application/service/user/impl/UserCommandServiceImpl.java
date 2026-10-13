package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.UserDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserDtoMapper userDtoMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Long createUser(UserDto userDto, String roleName) {
        return createUser(userDto, Set.of(roleName));
    }

    @Override
    public Long createUser(UserDto userDto, Set<String> roleNames) {
        if (userRepository.existsByEmail(userDto.email())) {
            throw new DuplicateResourceException("User with email " + userDto.email() + " already exists.");
        }

        var user = userDtoMapper.toUser(userDto);
        user.setPassword(passwordEncoder.encode(userDto.password()));
        User savedUser = userRepository.save(user, roleNames);
        return savedUser.getId();
    }

    @Override
    public void assignRolesToUser(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userRepository.save(user, roleNames);
    }
}
