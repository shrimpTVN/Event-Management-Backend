package com.ddd.application.mapper;

import com.ddd.application.dto.SemesterDto;
import com.ddd.domain.model.Semester;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SemesterDtoMapper {
    SemesterDto toDto(Semester semester);


    @Mapping(target = "id", ignore = true)
    @Mapping(target ="status", ignore = true)
    Semester toEntity(SemesterDto semesterDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(SemesterDto semesterDto, @MappingTarget Semester semester);
}
