package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Criteria;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CriteriaMapper {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "isActive", target = "active")
    @Mapping(target = "description", ignore = true)
    Criteria toDomain(CriteriaJpaEntity criteriaJpaEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parent", ignore = true)
    @Mapping(source = "active", target = "isActive")
    CriteriaJpaEntity toEntity(Criteria criteria);
}
