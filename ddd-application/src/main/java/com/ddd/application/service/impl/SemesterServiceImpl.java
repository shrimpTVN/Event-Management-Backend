package com.ddd.application.service.impl;

import com.ddd.application.dto.SemesterDto;
import com.ddd.application.mapper.SemesterDtoMapper;
import com.ddd.application.service.SemesterService;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;
    private final SemesterDtoMapper semesterDtoMapper;

    @Override
    public List<SemesterDto> findAll() {
        return semesterRepository.findAll().stream()
                .map(semesterDtoMapper::toDto).toList();
    }

    @Override
    public SemesterDto createSemester(SemesterDto semesterDto) {
        LocalDate now = LocalDate.now();

        if (semesterDto.startDate().isAfter(semesterDto.endDate())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        Semester semester = semesterDtoMapper.toEntity(semesterDto);
        if (semester.getStartDate().isAfter(now)){
            semester.setStatus("UPCOMING");
        } else if (semester.getEndDate().isBefore(now)) {
            semester.setStatus("FINISHED");
        } else {
            semester.setStatus("ONGOING");
        }

        return semesterDtoMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public SemesterDto findById(Long id) {
        return semesterDtoMapper.toDto(semesterRepository.findById(id));
    }

    @Override
    public SemesterDto getCurrent() {
        return semesterDtoMapper.toDto(semesterRepository.getCurrent());
    }

    @Override
    public SemesterDto updateSemester(Long id, SemesterDto semesterDto) {
        Semester existingSemester = semesterRepository.findById(id);
        if (existingSemester == null) {
            return createSemester(semesterDto);
        }

        semesterDtoMapper.updateEntityFromDto(semesterDto, existingSemester);
        return semesterDtoMapper.toDto(semesterRepository.save(existingSemester));
    }

    @Override
    public void deleteSemester(Long id) {
        semesterRepository.delete(id);
    }
}
