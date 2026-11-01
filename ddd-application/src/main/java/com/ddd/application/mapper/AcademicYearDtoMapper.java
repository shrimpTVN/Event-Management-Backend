package com.ddd.application.mapper;

import com.ddd.application.dto.academicyear.AcademicYearDto;
import com.ddd.domain.model.AcademicYear;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AcademicYearDtoMapper {
    AcademicYearDto toDto(AcademicYear academicYear);

    AcademicYear toEntity(AcademicYearDto dto);

    List<AcademicYearDto> toDtoList(List<AcademicYear> academicYears);
}
