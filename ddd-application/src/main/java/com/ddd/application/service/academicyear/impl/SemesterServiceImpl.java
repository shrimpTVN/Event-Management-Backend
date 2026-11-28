package com.ddd.application.service.academicyear.impl;

import com.ddd.application.dto.SemesterDto;
import com.ddd.application.mapper.SemesterDtoMapper;
import com.ddd.application.service.academicyear.AcademicYearService;
import com.ddd.application.service.academicyear.SemesterService;
import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AcademicYearService academicYearService;
    private final SemesterDtoMapper semesterDtoMapper;
    private final com.ddd.application.service.semesterpoint.impl.PointConfigurationRefresh pointRefresh;

    @Override
    public List<SemesterDto> findAll() {
        return toDtoListWithAcademicYear(semesterRepository.findAll());
    }

    @Override
    public List<SemesterDto> findAllActive() {
        return toDtoListWithAcademicYear(semesterRepository.findAllActive());
    }

    @Override
    public SemesterDto findById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));
        return toDtoWithAcademicYear(semester);
    }

    @Override
    @Transactional
    public SemesterDto getCurrent() {
        Semester semester = semesterRepository.getCurrent();
        AcademicYear academicYear = academicYearService.resolveOrCreateForSemester(semester);
        return toDtoWithAcademicYear(semester, academicYear.getCode());
    }

    @Override
    @Transactional
    public SemesterDto createSemester(SemesterDto semesterDto) {
        AcademicYear academicYear = resolveActiveAcademicYear(semesterDto);
        validateSemesterRules(semesterDto, academicYear, null);

        Semester semester = semesterDtoMapper.toEntity(semesterDto);
        semester.setAcademicYearId(academicYear.getId());
        semester.setStatus(resolveStatus(semester.getStartDate(), semester.getEndDate()));

        return toDtoWithAcademicYear(semesterRepository.save(semester), academicYear.getCode());
    }

    @Override
    @Transactional
    public SemesterDto updateSemester(Long id, SemesterDto semesterDto) {
        var before = pointRefresh.capture();
        Semester existingSemester = semesterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));

        AcademicYear academicYear = resolveActiveAcademicYear(semesterDto);
        validateSemesterRules(semesterDto, academicYear, id);

        semesterDtoMapper.updateEntityFromDto(semesterDto, existingSemester);
        existingSemester.setAcademicYearId(academicYear.getId());
        existingSemester.setStatus(resolveStatus(existingSemester.getStartDate(), existingSemester.getEndDate()));

        var saved = semesterRepository.save(existingSemester);
        pointRefresh.refresh(before);
        return toDtoWithAcademicYear(saved, academicYear.getCode());
    }

    @Override
    @Transactional
    public void deleteSemester(Long id) {
        semesterRepository.delete(id);
    }

    private AcademicYear resolveActiveAcademicYear(SemesterDto dto) {
        AcademicYear academicYear;
        if (dto.academicYearId() != null) {
            academicYear = academicYearRepository.findById(dto.academicYearId())
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with id: " + dto.academicYearId()));
        } else if (dto.academicYear() != null && !dto.academicYear().isBlank()) {
            academicYear = academicYearRepository.findByCode(dto.academicYear().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Academic year not found with code: " + dto.academicYear()));
        } else {
            throw new BusinessException("Either academicYearId or academicYear is required");
        }
        if (!academicYear.isActive()) {
            throw new BusinessException("Academic year is not active: " + academicYear.getCode());
        }
        return academicYear;
    }

    private void validateSemesterRules(SemesterDto dto, AcademicYear academicYear, Long excludeId) {
        validateDateRange(dto.startDate(), dto.endDate());

        if (dto.startDate().isBefore(academicYear.getStartDate()) || dto.endDate().isAfter(academicYear.getEndDate())) {
            throw new BusinessException("Semester date range must fall within academic year " + academicYear.getCode()
                    + " (" + academicYear.getStartDate() + " to " + academicYear.getEndDate() + ")");
        }
        if (semesterRepository.existsActiveByAcademicYearIdAndNumber(academicYear.getId(), dto.number(), excludeId)) {
            throw new DuplicateResourceException("Semester number " + dto.number()
                    + " already exists in academic year " + academicYear.getCode());
        }
        if (semesterRepository.existsActiveOverlappingDateRange(dto.startDate(), dto.endDate(), excludeId)) {
            throw new DuplicateResourceException("Semester date range overlaps with an existing active semester");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (!startDate.isBefore(endDate)) {
            throw new BusinessException("Start date must be before end date");
        }
    }

    private List<SemesterDto> toDtoListWithAcademicYear(List<Semester> semesters) {
        Map<Long, String> codeById = academicYearRepository.findAllActive().stream()
                .collect(Collectors.toMap(AcademicYear::getId, AcademicYear::getCode, (a, b) -> a));
        return semesters.stream()
                .map(semester -> {
                    String code = semester.getAcademicYearId() == null ? null
                            : codeById.computeIfAbsent(semester.getAcademicYearId(),
                                    id -> academicYearRepository.findById(id).map(AcademicYear::getCode).orElse(null));
                    return toDtoWithAcademicYear(semester, code);
                })
                .toList();
    }

    private SemesterDto toDtoWithAcademicYear(Semester semester) {
        String code = semester.getAcademicYearId() == null ? null
                : academicYearRepository.findById(semester.getAcademicYearId())
                        .map(AcademicYear::getCode)
                        .orElse(null);
        return toDtoWithAcademicYear(semester, code);
    }

    private SemesterDto toDtoWithAcademicYear(Semester semester, String academicYearCode) {
        SemesterDto mapped = semesterDtoMapper.toDto(semester);
        return new SemesterDto(
                mapped.number(),
                academicYearCode,
                mapped.startDate(),
                mapped.endDate(),
                mapped.status(),
                mapped.id(),
                mapped.academicYearId()
        );
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
