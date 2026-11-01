package com.ddd.application.service.academicyear;

import com.ddd.application.dto.academicyear.AcademicYearDto;
import com.ddd.domain.model.AcademicYear;
import com.ddd.domain.model.Semester;

import java.util.List;

public interface AcademicYearService {
    List<AcademicYearDto> findAllActive();

    AcademicYearDto getCurrent();

    AcademicYear resolveOrCreateForSemester(Semester semester);
}
