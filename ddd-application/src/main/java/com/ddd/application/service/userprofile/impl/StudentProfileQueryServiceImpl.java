package com.ddd.application.service.userprofile.impl;

import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.service.userprofile.StudentProfileQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.StudentProfile;
import com.ddd.domain.repository.StudentProfileRepository;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentProfileJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentProfileQueryServiceImpl implements StudentProfileQueryService {

    private final StudentProfileJpaRepository studentProfileRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;

}
