package com.ddd.application.mapper;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import com.ddd.infrastructure.entity.StudentSemesterPointJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentSemesterPointDtoMapper {
    @Mapping(target="pointCategoryId", source="pointCategory.id")
    StudentSemesterPointInfoDto toDto(StudentSemesterPointJpaEntity entity);


}
