package com.ddd.application.mapper;

import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.domain.model.Criteria;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CriteriaDtoMapper {
    @Mapping(source = "isActive", target = "active")
    @Mapping(target = "description", ignore = true)
    Criteria toCriteria(CriteriaDto criteriaDto);

    @Mapping(source = "active", target = "isActive")
    CriteriaDto toDto(Criteria criteria);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "isActive", target = "active")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromDto(@MappingTarget Criteria criteria, CriteriaDto dto);
}

