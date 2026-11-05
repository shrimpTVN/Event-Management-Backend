package com.ddd.application.mapper;

import com.ddd.application.dto.academicyear.AcademicYearDto;
import com.ddd.domain.model.AcademicYear;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AcademicYearDtoMapper {
    @Mapping(source = "active", target = "isActive")
    AcademicYearDto toDto(AcademicYear academicYear);

    @Mapping(source = "isActive", target = "active")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AcademicYear toEntity(AcademicYearDto dto);

    List<AcademicYearDto> toDtoList(List<AcademicYear> academicYears);
}
