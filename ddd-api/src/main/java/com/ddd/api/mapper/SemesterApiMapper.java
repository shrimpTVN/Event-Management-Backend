package com.ddd.api.mapper;

import com.ddd.api.dto.auth.req.SemesterRequestDto;
import com.ddd.api.dto.auth.res.SemesterResponseDto;
import com.ddd.application.dto.SemesterDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SemesterApiMapper {
    SemesterResponseDto toResponseDto(SemesterDto semesterDto);
    SemesterDto toDto(SemesterRequestDto semesterRequestDto);
}
