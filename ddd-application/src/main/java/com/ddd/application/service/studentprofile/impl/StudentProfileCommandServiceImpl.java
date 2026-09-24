package com.ddd.application.service.studentprofile.impl;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.service.studentprofile.StudentProfileCommandService;
import com.ddd.domain.model.StudentProfile;
import com.ddd.domain.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StudentProfileCommandServiceImpl implements StudentProfileCommandService {
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileDtoMapper studentProfileDtoMapper;

    @Override
    public void createStudentProfile(StudentProfileDto studentProfileDto, Long userId) {
       if (studentProfileRepository.existsByStudentId(studentProfileDto.studentId())) {
           throw new IllegalArgumentException("Student ID already exists");
       }
       StudentProfile studentProfile = studentProfileDtoMapper.toStudentProfile(studentProfileDto);
       studentProfileRepository.save(studentProfile, userId);
    }
}
