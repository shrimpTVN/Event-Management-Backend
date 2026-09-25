package com.ddd.api.mapper;

import com.ddd.api.dto.user.res.StudentProfileResponseDto;
import com.ddd.application.dto.user.StudentProfileSummaryDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentProfileApiMapper {
    StudentProfileResponseDto toStudentProfileResponseDto(StudentProfileSummaryDto studentProfile);
}
