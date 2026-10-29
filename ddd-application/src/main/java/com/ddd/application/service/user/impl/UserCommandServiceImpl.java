package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileUpdateDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.MajorJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
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
    private final UserJpaRepository userJpaRepository;
    private final StudentProfileJpaRepository studentProfileJpaRepository;
    private final MajorJpaRepository majorJpaRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;

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
        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }
        User savedUser = userRepository.save(user, roleNames);
        return savedUser.getId();
    }

    @Override
    public void assignRolesToUser(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userRepository.save(user, roleNames);
    }

    @Override
    public void changePassword(String oldpPassword, String newPassword, String email) {
        User user = userRepository.findByEmail(email);

        if (!passwordEncoder.matches(oldpPassword, user.getPassword())) {
            throw new IllegalArgumentException("Old password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public StudentProfileSummaryDto updateStudentProfile(String email, StudentProfileUpdateDto updateDto) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for email: " + email);
        }

        updateUserFields(user, updateDto);
        userJpaRepository.save(user);

        StudentProfileJpaEntity profile = studentProfileJpaRepository.findByEmail(email)
                .orElseGet(() -> {
                    StudentProfileJpaEntity newProfile = new StudentProfileJpaEntity();
                    newProfile.setUser(user);
                    return newProfile;
                });

        updateProfileFields(profile, updateDto);
        studentProfileJpaRepository.save(profile);

        StudentProfileJpaEntity savedProfile = studentProfileJpaRepository.findByEmail(email)
                .orElse(profile);
        return studentProfileDtoMapper.toStudentProfileSummaryDto(savedProfile);
    }

    @Override
    public StudentProfileSummaryDto updateStudentProfileById(Long id, StudentProfileUpdateDto updateDto) {
        UserJpaEntity user = userJpaRepository.findById(id)
                .orElseGet(() -> {
                    StudentProfileJpaEntity p = studentProfileJpaRepository.findByIdWithDetails(id)
                            .orElseThrow(() -> new ResourceNotFoundException("User or Student profile not found for id: " + id));
                    return p.getUser();
                });

        updateUserFields(user, updateDto);
        userJpaRepository.save(user);

        StudentProfileJpaEntity profile = studentProfileJpaRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    StudentProfileJpaEntity newProfile = new StudentProfileJpaEntity();
                    newProfile.setUser(user);
                    return newProfile;
                });

        updateProfileFields(profile, updateDto);
        studentProfileJpaRepository.save(profile);

        StudentProfileJpaEntity savedProfile = studentProfileJpaRepository.findByUserId(user.getId())
                .orElse(profile);
        return studentProfileDtoMapper.toStudentProfileSummaryDto(savedProfile);
    }

    @Override
    public StudentProfileSummaryDto updateAvatar(String email, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("Avatar URL must not be blank");
        }

        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for email: " + email);
        }

        user.setAvatarUrl(avatarUrl.trim());
        userJpaRepository.save(user);

        StudentProfileJpaEntity profile = studentProfileJpaRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for email: " + email));
        return studentProfileDtoMapper.toStudentProfileSummaryDto(profile);
    }

    @Override
    public UserSummaryDto updateProfileStatusById(Long id) {
        UserJpaEntity user = userJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        boolean newStatus = user.getIsActive() == null || !user.getIsActive();
        user.setIsActive(newStatus);
        UserJpaEntity savedUser = userJpaRepository.save(user);

        studentProfileJpaRepository.findByUserId(user.getId()).ifPresent(profile -> {
            profile.setIsActive(newStatus);
            studentProfileJpaRepository.save(profile);
        });

        return userDtoMapper.toUserSummaryDto(savedUser);
    }

    private void updateUserFields(UserJpaEntity user, StudentProfileUpdateDto updateDto) {
        if (updateDto.firstName() != null && !updateDto.firstName().isBlank()) {
            user.setFirstName(updateDto.firstName().trim());
        }
        if (updateDto.lastName() != null && !updateDto.lastName().isBlank()) {
            user.setLastName(updateDto.lastName().trim());
        }
        if (updateDto.gender() != null && !updateDto.gender().isBlank()) {
            user.setGender(updateDto.gender().trim());
        }
        if (updateDto.avatarUrl() != null && !updateDto.avatarUrl().isBlank()) {
            user.setAvatarUrl(updateDto.avatarUrl().trim());
        }
    }

    private void updateProfileFields(StudentProfileJpaEntity profile, StudentProfileUpdateDto updateDto) {
        if (updateDto.kNumber() != null) {
            profile.setKNumber(updateDto.kNumber());
        }
        if (updateDto.majorId() != null) {
            MajorJpaEntity major = majorJpaRepository.findById(Long.valueOf(updateDto.majorId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Major not found with id: " + updateDto.majorId()));
            profile.setMajor(major);
        }
    }
}

