package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.mapper.UserDtoMapper;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private final StudentProfileJpaRepository studentProfileJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;
    private final UserDtoMapper userDtoMapper;

    @Override
    public StudentProfileSummaryDto getProfileByEmail(String email) {
        StudentProfileJpaEntity studentProfileJpaEntity = studentProfileJpaRepository.findByEmail(email);
        if (studentProfileJpaEntity == null) {
            throw new ResourceNotFoundException("Student profile not found for email: " + email);
        }
        return studentProfileDtoMapper.toStudentProfileSummaryDto(studentProfileJpaEntity);
    }

    @Override
    public Page<StudentProfileSummaryDto> getAllProfiles(boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);

        return studentProfileJpaRepository.findAllByIsActive(isActive, pageable)
                .map(studentProfileDtoMapper::toStudentProfileSummaryDto);
    }

    @Override
    public Page<UserSummaryDto> getAllUsers(boolean isActive, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        return userJpaRepository.findAllByIsActive(isActive, pageable)
                .map(userDtoMapper::toUserSummaryDto);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }
}
