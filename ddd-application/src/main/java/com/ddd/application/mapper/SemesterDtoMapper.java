package com.ddd.application.mapper;

import com.ddd.application.dto.auth.SemesterDto;
import com.ddd.domain.model.Semester;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SemesterDtoMapper {
    SemesterDto toDto(Semester semester);
}
