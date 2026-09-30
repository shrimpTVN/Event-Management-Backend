package com.ddd.application.service.userprofile.impl;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.mapper.StudentProfileDtoMapper;
import com.ddd.application.service.userprofile.StudentProfileCommandService;
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

    @Override
    public void updateProfileByEmail(String email, StudentProfileDto studentProfileDto) {
        StudentProfile studentProfile = studentProfileRepository.findByEmail(email);
        studentProfileDtoMapper.updateEntityFromDto(studentProfile, studentProfileDto);
        studentProfileRepository.save(studentProfile, studentProfile.getUserId());
    }

    @Override
    public void updateAvatarByEmail(String email, String avatarUrl){
        StudentProfile studentProfile = studentProfileRepository.findByEmail(email);
        studentProfile.setAvatarUrl(avatarUrl);
        studentProfileRepository.save(studentProfile, studentProfile.getUserId());
    }

    @Override
    public void updateProfileById(Long id, StudentProfileDto studentProfileDto){
        StudentProfile studentProfile = studentProfileRepository.findByUserId(id);
        studentProfileDtoMapper.updateEntityFromDto(studentProfile, studentProfileDto);
        studentProfileRepository.save(studentProfile, studentProfile.getUserId());
    }

}
