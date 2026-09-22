package com.ddd.api.mapper;

import com.ddd.api.dto.auth.res.SemesterResponseDto;
import com.ddd.application.dto.auth.SemesterDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SemesterApiMapper {
    SemesterResponseDto toResponseDto(SemesterDto semesterDto);

}
