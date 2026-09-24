package com.ddd.application.mapper;

import com.ddd.application.dto.user.StudentProfileDto;
import com.ddd.domain.model.StudentProfile;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentProfileDtoMapper {
    @Mapping(source = "KNumber", target = "kNumber")
    StudentProfileDto toStudentProfileDto(@Valid StudentProfile  studentProfile);

    @Mapping(source = "kNumber", target = "KNumber")
    StudentProfile toStudentProfile(StudentProfileDto studentProfileDto);
}
