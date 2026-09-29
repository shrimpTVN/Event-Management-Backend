package com.ddd.application.mapper;

import com.ddd.application.dto.criteria.CriteriaDto;
import com.ddd.domain.model.Criteria;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CriteriaDtoMapper {
    @Mapping(source = "isActive", target = "active")
    Criteria toCriteria(CriteriaDto criteriaDto);

    @Mapping(source = "isActive", target = "isActive")
    CriteriaDto toCriteriaDto(CriteriaJpaEntity criteriaJpaEntity);
    
    @Mapping(source = "active", target = "isActive")
    CriteriaDto toDto(Criteria criteria);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "isActive", target = "active")
    void updateEntityFromDto(@MappingTarget Criteria criteria, CriteriaDto dto);
}
