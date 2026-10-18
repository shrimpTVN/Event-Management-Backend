package com.ddd.api.mapper;

import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointResponseDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentSemesterPointApiMapper {
    StudentSemesterPointResponseDto toResponseDto(StudentSemesterPointInfoDto dto);
}
