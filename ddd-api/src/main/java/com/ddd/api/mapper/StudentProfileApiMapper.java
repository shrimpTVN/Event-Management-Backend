package com.ddd.api.mapper;

import com.ddd.api.dto.user.req.StudentProfileRequestDto;
import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import com.ddd.domain.model.StudentProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentProfileApiMapper {
    StudentProfileResponseDto toStudentProfileResponseDto(StudentProfileSummaryDto studentProfile);
    StudentProfileDto toStudentProfileDto(StudentProfileRequestDto studentProfileRequestDto);
}
