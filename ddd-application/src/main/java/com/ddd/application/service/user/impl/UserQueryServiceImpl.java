package com.ddd.application.service.user.impl;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.service.user.UserQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {
    private final StudentProfileJpaRepository studentProfileJpaRepository;


    private final StudentProfileDtoMapper studentProfileDtoMapper;

    @Override
    public StudentProfileSummaryDto getProfileByEmail(String email) {
        StudentProfileJpaEntity studentProfileJpaEntity = studentProfileJpaRepository.findByEmail(email);
        if (studentProfileJpaEntity == null) {
            throw new ResourceNotFoundException("Student profile not found for email: " + email);
        }
        return studentProfileDtoMapper.toStudentProfileSummaryDto(studentProfileJpaEntity);
    }
}
