package com.ddd.api.mapper;

import com.ddd.api.dto.semester.SemesterRequestDto;
import com.ddd.api.dto.semester.SemesterResponseDto;
import com.ddd.application.dto.SemesterDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SemesterApiMapper {
    SemesterResponseDto toResponseDto(SemesterDto semesterDto);
    SemesterDto toDto(SemesterRequestDto semesterRequestDto);
}
