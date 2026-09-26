package com.ddd.application.mapper;

import com.ddd.application.dto.school.MajorDto;
import com.ddd.domain.model.Major;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MajorDtoMapper {
    MajorDto toMajorDto(Major major);

    @Mapping(target = "id", ignore = true)
    Major toMajorEntity(MajorDto majorDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMajorEntityFromDto(@MappingTarget Major major, MajorDto majorDto);
}
