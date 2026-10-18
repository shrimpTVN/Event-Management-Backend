package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import com.ddd.application.mapper.StudentSemesterPointDtoMapper;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSemesterPointQueryServiceImpl implements StudentSemesterPointQueryService {
    private final StudentSemesterPointJpaRepository studentSemesterPointJpaRepository;
    private final StudentSemesterPointDtoMapper studentSemesterPointDtoMapper;
    private final UserJpaRepository userJpaRepository;

    @Override
    public List<StudentSemesterPointInfoDto> getSemesterPointsForStudent(String email, Long semesterId) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        return studentSemesterPointJpaRepository.findByUserIdAndSemesterId(user.getId(), semesterId)
                .stream().map(studentSemesterPointDtoMapper::toDto).collect(Collectors.toList());
    }
}
