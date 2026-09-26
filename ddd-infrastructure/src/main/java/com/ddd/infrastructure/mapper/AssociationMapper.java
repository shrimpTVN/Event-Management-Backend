package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Association;
import com.ddd.infrastructure.entity.AssociationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssociationMapper {
    @Mapping(source = "active", target = "isActive")
    AssociationJpaEntity toEntity(Association association);
    
    @Mapping(source = "isActive", target = "active")
    Association toDomain(AssociationJpaEntity entity);
}
