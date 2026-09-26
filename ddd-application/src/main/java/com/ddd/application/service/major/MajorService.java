package com.ddd.application.service.major;

import com.ddd.application.dto.school.MajorDto;
import com.ddd.domain.model.Major;

import java.util.List;

public interface MajorService {
    List<MajorDto> findAll();
    MajorDto findById(Long id);
    MajorDto updateMajor(Long id, MajorDto majorDto);
    MajorDto createMajor(MajorDto majorDto);
    void deleteMajor(Long id);

}
