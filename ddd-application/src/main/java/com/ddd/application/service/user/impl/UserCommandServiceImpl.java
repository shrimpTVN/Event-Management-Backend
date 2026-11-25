package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StaffProfileDetailsUpdateDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.application.dto.user.StaffProfileUpdateDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.StudentProfileUpdateDto;
import com.ddd.application.dto.user.UserDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.mapper.StaffProfileDtoMapper;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserCommandService;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.PasswordAlreadySetException;
import com.ddd.domain.exception.PasswordNotSetException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageAdminProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.MajorJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {
    private final UserDtoMapper userDtoMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserJpaRepository userJpaRepository;
    private final StudentProfileJpaRepository studentProfileJpaRepository;
    private final FanpageAdminProfileJpaRepository fanpageAdminProfileJpaRepository;
    private final MajorJpaRepository majorJpaRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;
    private final StaffProfileDtoMapper staffProfileDtoMapper;
    private final SecurityContextPort securityContextPort;
    private final FanpageMemberRepository fanpageMemberRepository;

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
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new PasswordNotSetException();
        }

        if (!passwordEncoder.matches(oldpPassword, user.getPassword())) {
            throw new AuthException("Old password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void createPassword(String newPassword) {
        Long userId = securityContextPort.getCurrentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Authentication required to create a password");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        boolean updated = userRepository.setPasswordIfAbsent(userId, encodedPassword);
        if (!updated) {
            throw new PasswordAlreadySetException();
        }
        log.info("Password created successfully for user id: {}", userId);
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
    public UserSummaryDto updateAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("Avatar URL must not be blank");
        }

        Long currentUserId = securityContextPort.getCurrentUserId();
        UserJpaEntity user = null;
        if (currentUserId != null) {
            user = userJpaRepository.findById(currentUserId).orElse(null);
        }
        if (user == null) {
            String email = securityContextPort.getCurrentUserEmail();
            if (email != null) {
                user = userJpaRepository.findByEmail(email);
            }
        }
        if (user == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        user.setAvatarUrl(avatarUrl.trim());
        UserJpaEntity savedUser = userJpaRepository.save(user);

        return userDtoMapper.toUserSummaryDto(savedUser);
    }

    @Override
    public UserSummaryDto updateAvatar(String email, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("Avatar URL must not be blank");
        }

        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for email: " + email);
        }

        user.setAvatarUrl(avatarUrl.trim());
        UserJpaEntity savedUser = userJpaRepository.save(user);

        return userDtoMapper.toUserSummaryDto(savedUser);
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

    @Override
    public void outFanpageMember(Long fanpageId) {
        Long userId = securityContextPort.getCurrentUserId();
        FanpageMember fanpageMember = fanpageMemberRepository.findByFanpageIdAndUserId(fanpageId, userId);

        if (fanpageMember == null || !fanpageMember.isActive()) {
            throw new ResourceNotFoundException("You are not a member of this fanpage");
        }

        if (fanpageMember.getRole().equals(FanpageRoleEnum.ADMIN.name())) {
            throw new IllegalArgumentException("Admin cannot leave the fanpage. Please transfer admin role before leaving.");
        }

        fanpageMemberRepository.removeFanpageMember(fanpageMember);
        log.info("User with ID {} has left the fanpage with ID {}", userId, fanpageId);
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
            if (!Boolean.TRUE.equals(major.getIsActive())) {
                throw new BusinessException("Selected major is not active");
            }
            if (major.getSchool() == null || !Boolean.TRUE.equals(major.getSchool().getIsActive())) {
                throw new BusinessException("Selected major's school is not active");
            }
            if (updateDto.schoolId() != null && !major.getSchool().getId().equals(Long.valueOf(updateDto.schoolId()))) {
                throw new BusinessException("Selected major does not belong to the selected school");
            }
            profile.setMajor(major);
        }
    }

    @Override
    public StaffProfileSummaryDto updateStaffProfile(StaffProfileUpdateDto updateDto) {
        if (updateDto == null) {
            throw new BusinessException("Update payload cannot be null");
        }

        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        UserJpaEntity user = userJpaRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        // 1. Partial update of User entity
        if (updateDto.isFirstNamePresent()) {
            if (updateDto.getFirstName() == null || updateDto.getFirstName().trim().isEmpty()) {
                throw new BusinessException("First name cannot be blank");
            }
            user.setFirstName(updateDto.getFirstName().trim());
        }

        if (updateDto.isLastNamePresent()) {
            if (updateDto.getLastName() == null || updateDto.getLastName().trim().isEmpty()) {
                throw new BusinessException("Last name cannot be blank");
            }
            user.setLastName(updateDto.getLastName().trim());
        }

        if (updateDto.isGenderPresent()) {
            if (updateDto.getGender() == null) {
                throw new BusinessException("Gender cannot be null");
            }
            String gender = updateDto.getGender().trim().toUpperCase();
            if (!Set.of("MALE", "FEMALE", "OTHER").contains(gender)) {
                throw new BusinessException("Gender must be MALE, FEMALE, or OTHER");
            }
            user.setGender(gender);
        }

        userJpaRepository.save(user);

        // 2. Partial update of FanpageAdminProfile entity
        FanpageAdminProfileJpaEntity staffProfile = null;
        if (updateDto.isStaffProfilePresent()) {
            if (updateDto.getStaffProfile() != null) {
                staffProfile = fanpageAdminProfileJpaRepository.findByUserId(currentUserId)
                        .orElseThrow(() -> new ResourceNotFoundException("Staff profile not found for user id: " + currentUserId));

                StaffProfileDetailsUpdateDto details = updateDto.getStaffProfile();
                if (details.isTitlePresent()) {
                    staffProfile.setTitle(details.getTitle() != null ? details.getTitle().trim() : null);
                }
                if (details.isDateOfBirthPresent()) {
                    if (details.getDateOfBirth() != null && details.getDateOfBirth().isAfter(java.time.LocalDate.now())) {
                        throw new BusinessException("Date of birth cannot be in the future");
                    }
                    staffProfile.setDob(details.getDateOfBirth());
                }
                if (details.isPhoneNumberPresent()) {
                    staffProfile.setPhoneNumber(details.getPhoneNumber() != null ? details.getPhoneNumber().trim() : null);
                }
                staffProfile = fanpageAdminProfileJpaRepository.save(staffProfile);
            }
        } else {
            staffProfile = fanpageAdminProfileJpaRepository.findByUserId(currentUserId).orElse(null);
        }

        return staffProfileDtoMapper.toStaffProfileSummaryDto(user, staffProfile);
    }
}

