package com.ddd.application.service.academicyear;

import com.ddd.application.dto.SemesterDto;

import java.util.List;

public interface SemesterService {
    List<SemesterDto> findAll();

    SemesterDto createSemester(SemesterDto semesterDto);

    SemesterDto findById(Long id);

    SemesterDto getCurrent();

    SemesterDto updateSemester(Long id, SemesterDto semesterDto);

    void deleteSemester(Long id);

    List<SemesterDto> findAllActive();
}
