package com.ddd.application.service.school.impl;
import com.ddd.application.dto.school.*;
import com.ddd.application.mapper.SchoolDtoMapper;
import com.ddd.application.service.school.SchoolService;
import com.ddd.domain.model.School;
import com.ddd.domain.model.Major;
import com.ddd.domain.repository.SchoolRepository;
import com.ddd.domain.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchoolServiceImpl implements SchoolService {
    private final SchoolRepository schoolRepository;
    private final MajorRepository majorRepository;
    private final SchoolDtoMapper schoolDtoMapper;

    @Override
    public List<SchoolDto> findAll() {
        return schoolRepository.findAll()
                .stream()
                .map(schoolDtoMapper::toSchoolDto)
                .toList();
    }

    @Override
    public SchoolDetailDto findById(Long id) {
        School school = schoolRepository.findById(id);
        List<Major> majors = majorRepository.findBySchoolId(id);
        return schoolDtoMapper.toDetailSchoolDto(school, majors);
    }
    @Override
    @Transactional
    public SchoolDto createSchool(SchoolDto schoolDto) {
        School school = schoolDtoMapper.toSchoolEntity(schoolDto);
        return schoolDtoMapper.toSchoolDto(schoolRepository.save(school));
    }
    @Override
    @Transactional
    public SchoolDto updateSchool(Long id, SchoolDto schoolDto) {
        School school = schoolRepository.findById(id);
        schoolDtoMapper.updateSchoolEntityFromDto(schoolDto, school);
        return schoolDtoMapper.toSchoolDto(schoolRepository.save(school));
    }
    @Override
    @Transactional
    public void deleteSchool(Long id) {
        schoolRepository.deleteSchool(id);
    }
}