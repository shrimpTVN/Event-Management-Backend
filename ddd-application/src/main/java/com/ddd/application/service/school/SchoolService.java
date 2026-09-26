package com.ddd.application.service.school;
import com.ddd.application.dto.school.SchoolDto;
import com.ddd.application.dto.school.SchoolDetailDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchoolService {

    List<SchoolDto> findAll();
    SchoolDetailDto findById(Long id);
    SchoolDto createSchool(SchoolDto schoolDto);
    SchoolDto updateSchool(Long id, SchoolDto schoolDto);
    void deleteSchool(Long id);
}