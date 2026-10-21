package com.ddd.application.service.academicyear.impl;

import com.ddd.application.dto.academicyear.AcademicYearDto;
import com.ddd.application.mapper.AcademicYearDtoMapper;
import com.ddd.application.service.academicyear.AcademicYearService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.AcademicYear;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.AcademicYearRepository;
import com.ddd.domain.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicYearServiceImpl implements AcademicYearService {
    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;
    private final AcademicYearDtoMapper academicYearDtoMapper;

    @Override
    public List<AcademicYearDto> findAllActive() {
        return academicYearDtoMapper.toDtoList(academicYearRepository.findAllActive());
    }

    @Override
    @Transactional
    public AcademicYearDto getCurrent() {
        AcademicYear current = academicYearRepository.findCurrent(LocalDate.now())
                .orElseGet(this::ensureFromCurrentSemester);
        return academicYearDtoMapper.toDto(current);
    }

    @Override
    @Transactional
    public AcademicYear resolveOrCreateForSemester(Semester semester) {
        if (semester == null) {
            throw new ResourceNotFoundException("Semester cannot be null");
        }
        if (semester.getAcademicYearId() != null) {
            return academicYearRepository.findById(semester.getAcademicYearId())
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        }

        AcademicYear academicYear = academicYearRepository.findByCode(semester.getAcademicYear())
                .orElseGet(() -> createAcademicYear(semester));
        semester.setAcademicYearId(academicYear.getId());
        semesterRepository.save(semester);
        return academicYear;
    }

    private AcademicYear ensureFromCurrentSemester() {
        Semester semester = semesterRepository.getCurrent();
        return resolveOrCreateForSemester(semester);
    }

    private AcademicYear createAcademicYear(Semester semester) {
        AcademicYear academicYear = new AcademicYear();
        academicYear.setCode(semester.getAcademicYear());
        academicYear.setStartDate(semester.getStartDate());
        academicYear.setEndDate(semester.getEndDate());
        return academicYearRepository.save(academicYear);
    }
}
