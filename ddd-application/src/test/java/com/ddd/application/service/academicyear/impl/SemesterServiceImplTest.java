package com.ddd.application.service.academicyear.impl;

import com.ddd.application.dto.SemesterDto;
import com.ddd.application.mapper.SemesterDtoMapper;
import com.ddd.application.service.academicyear.AcademicYearService;
import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.AcademicYear;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.AcademicYearRepository;
import com.ddd.domain.repository.SemesterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterServiceImplTest {

    @Mock
    private com.ddd.application.service.semesterpoint.impl.PointConfigurationRefresh pointRefresh;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private AcademicYearService academicYearService;

    private final SemesterDtoMapper semesterDtoMapper = Mappers.getMapper(SemesterDtoMapper.class);

    private SemesterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SemesterServiceImpl(semesterRepository, academicYearRepository, academicYearService, semesterDtoMapper, pointRefresh);
    }

    private AcademicYear activeYear() {
        AcademicYear year = new AcademicYear();
        year.setId(1L);
        year.setCode("2025-2026");
        year.setStartDate(LocalDate.of(2025, 8, 1));
        year.setEndDate(LocalDate.of(2026, 7, 31));
        year.setActive(true);
        return year;
    }

    @Test
    void createSemester_shouldPopulateAcademicYearCode_whenAcademicYearIdProvided() {
        AcademicYear year = activeYear();
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(semesterRepository.existsActiveByAcademicYearIdAndNumber(1L, 1, null)).thenReturn(false);
        when(semesterRepository.existsActiveOverlappingDateRange(
                LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31), null)).thenReturn(false);
        when(semesterRepository.save(any(Semester.class))).thenAnswer(inv -> {
            Semester s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SemesterDto input = new SemesterDto(1, null, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31), null, null, 1L);
        SemesterDto created = service.createSemester(input);

        assertEquals("2025-2026", created.academicYear());
        assertEquals(1L, created.academicYearId());
        assertEquals(10L, created.id());
    }

    @Test
    void createSemester_shouldThrowBusinessException_whenOutsideAcademicYearBounds() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(activeYear()));

        SemesterDto input = new SemesterDto(1, null, LocalDate.of(2025, 7, 15), LocalDate.of(2025, 12, 31), null, null, 1L);

        assertThrows(BusinessException.class, () -> service.createSemester(input));
    }

    @Test
    void createSemester_shouldThrowDuplicateResourceException_whenDuplicateNumberInAcademicYear() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(activeYear()));
        when(semesterRepository.existsActiveByAcademicYearIdAndNumber(1L, 1, null)).thenReturn(true);

        SemesterDto input = new SemesterDto(1, null, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31), null, null, 1L);

        assertThrows(DuplicateResourceException.class, () -> service.createSemester(input));
    }

    @Test
    void createSemester_shouldThrowDuplicateResourceException_whenOverlappingActiveSemester() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(activeYear()));
        when(semesterRepository.existsActiveByAcademicYearIdAndNumber(1L, 2, null)).thenReturn(false);
        when(semesterRepository.existsActiveOverlappingDateRange(
                LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 31), null)).thenReturn(true);

        SemesterDto input = new SemesterDto(2, null, LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 31), null, null, 1L);

        assertThrows(DuplicateResourceException.class, () -> service.createSemester(input));
    }

    @Test
    void findAll_shouldPopulateAcademicYearCode() {
        Semester semester = new Semester();
        semester.setId(5L);
        semester.setNumber(1);
        semester.setAcademicYearId(1L);
        semester.setStartDate(LocalDate.of(2025, 9, 1));
        semester.setEndDate(LocalDate.of(2025, 12, 31));
        semester.setStatus(SemesterStatusEnum.COMPLETED);

        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        when(academicYearRepository.findAllActive()).thenReturn(List.of(activeYear()));

        List<SemesterDto> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals("2025-2026", result.get(0).academicYear());
    }
}
