package com.ddd.application.mapper;

import com.ddd.application.dto.association.AssociationDto;
import com.ddd.domain.model.Association;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AssociationDtoMapper {
    @Mapping(source = "active", target = "isActive")
    AssociationDto toDto(Association association);

    @Mapping(source = "isActive", target = "active")
    Association toEntity(AssociationDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "isActive", target = "active")
    void updateEntityFromDto(@MappingTarget Association association, AssociationDto dto);
}
