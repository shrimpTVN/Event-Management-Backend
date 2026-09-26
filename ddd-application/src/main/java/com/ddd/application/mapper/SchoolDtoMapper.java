package com.ddd.application.mapper;

import com.ddd.application.dto.school.*;
import com.ddd.domain.model.School;
import com.ddd.domain.model.Major;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SchoolDtoMapper {
    SchoolDto toSchoolDto(School school);
    MajorDto toMajorDto(Major major);

    @Mapping(source = "school.id", target = "id")
    @Mapping(source = "school.name", target = "name")
    @Mapping(source = "school.description", target = "description")
    @Mapping(source = "school.active", target = "isActive")
    @Mapping(source = "majors", target = "majors")
    SchoolDetailDto toDetailSchoolDto(School school, List<Major> majors);

    @Mapping(target = "id", ignore = true)
    School toSchoolEntity(SchoolDto schoolDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSchoolEntityFromDto(SchoolDto schoolDto, @MappingTarget School school);
}
