package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.PointCategory;
import com.ddd.infrastructure.entity.PointCategoryJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PointCategoryMapper {

    @Mapping(source = "parentCategory.id", target = "parentCategoryId")
    @Mapping(source = "isActive", target = "active")
    PointCategory toDomainModel(PointCategoryJpaEntity entity);

    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(source = "active", target = "isActive")
    PointCategoryJpaEntity toJpaEntity(PointCategory domain);
}
