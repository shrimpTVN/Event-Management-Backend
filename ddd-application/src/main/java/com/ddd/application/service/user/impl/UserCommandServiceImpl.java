package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.Role;
import com.ddd.domain.model.StudentProfile;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.RoleRepository;
import com.ddd.domain.repository.StudentProfileRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
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
    private final UserJpaRepository userJpaRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RoleRepository roleRepository;


    @Override
    public Long createUser(UserDto userDto, String roleName) {
        if (userRepository.existsByEmail(userDto.email())) {
            throw new DuplicateResourceException("User with email " + userDto.email() + " already exists.");
        }

        var user = userDtoMapper.toUser(userDto);
        user.setPassword(passwordEncoder.encode(userDto.password()));
        User savedUser = userRepository.save(user, roleName);
        return savedUser.getId();
    }

    @Override
    public void changeStatusUser(Long id){
        User user = userRepository.findById(id);
        user.setActive(!user.isActive());
        Role role = roleRepository.findById(user.getRoleId());
        userRepository.save(user, role.getName());
    }

    @Override
    public void changeRoleUser(Long id, String roleName) {
        User user = userRepository.findById(id);
        Role role = roleRepository.findByName(roleName);
        user.setRoleId(role.getId());
        userRepository.save(user, roleName);
    }
}
