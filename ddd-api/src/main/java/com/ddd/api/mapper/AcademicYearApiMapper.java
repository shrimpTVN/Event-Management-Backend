package com.ddd.api.mapper;

import com.ddd.api.dto.academicyear.res.AcademicYearResponseDto;
import com.ddd.application.dto.academicyear.AcademicYearDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AcademicYearApiMapper {
    AcademicYearResponseDto toResponseDto(AcademicYearDto dto);

    List<AcademicYearResponseDto> toResponseDtoList(List<AcademicYearDto> dtos);
}
