package com.ddd.application.service.impl;

import com.ddd.application.dto.SemesterDto;
import com.ddd.application.mapper.SemesterDtoMapper;
import com.ddd.application.service.SemesterService;
import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;
    private final SemesterDtoMapper semesterDtoMapper;

    @Override
    public List<SemesterDto> findAll() {
        return semesterRepository.findAll().stream()
                .map(semesterDtoMapper::toDto).toList();
    }

    @Override
    @Transactional
    public SemesterDto createSemester(SemesterDto semesterDto) {
        validateDateRange(semesterDto.startDate(), semesterDto.endDate());

        if (semesterRepository.existsByNumberAndDateRange(semesterDto.number(), semesterDto.startDate(), semesterDto.endDate())) {
            throw new DuplicateResourceException("Semester already exists with the same number and date range");
        }

        if (semesterRepository.existsOverlappingDateRange(semesterDto.startDate(), semesterDto.endDate())) {
            throw new DuplicateResourceException("Semester date range overlaps with an existing semester");
        }

        Semester semester = semesterDtoMapper.toEntity(semesterDto);
        semester.setStatus(resolveStatus(semester.getStartDate(), semester.getEndDate()));

        return semesterDtoMapper.toDto(semesterRepository.save(semester));
    }

    @Override
    public SemesterDto findById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));
        return semesterDtoMapper.toDto(semester);
    }

    @Override
    public SemesterDto getCurrent() {
        return semesterDtoMapper.toDto(semesterRepository.getCurrent());
    }

    @Override
    @Transactional
    public SemesterDto updateSemester(Long id, SemesterDto semesterDto) {
        Semester existingSemester = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));

        validateDateRange(semesterDto.startDate(), semesterDto.endDate());

        semesterDtoMapper.updateEntityFromDto(semesterDto, existingSemester);
        existingSemester.setStatus(resolveStatus(existingSemester.getStartDate(), existingSemester.getEndDate()));

        return semesterDtoMapper.toDto(semesterRepository.save(existingSemester));
    }

    @Override
    @Transactional
    public void deleteSemester(Long id) {
        semesterRepository.delete(id);
    }

    @Override
    public List<SemesterDto> findAllActive() {
        return semesterRepository.findAllActive().stream()
                .map(semesterDtoMapper::toDto).toList();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }
    }

    private SemesterStatusEnum resolveStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();
        if (startDate.isAfter(now)) {
            return SemesterStatusEnum.UPCOMING;
        } else if (endDate.isBefore(now)) {
            return SemesterStatusEnum.COMPLETED;
        } else {
            return SemesterStatusEnum.ONGOING;
        }
    }
}
