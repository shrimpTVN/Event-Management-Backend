package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.School;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SchoolMapper {
    @Mapping(source = "isActive", target = "active")
    School toDomainModel(SchoolJpaEntity schoolJpaEntity);

    @Mapping(source = "active", target = "isActive")
    SchoolJpaEntity toJpaEntity(School school);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "active", target = "isActive")
    void updateJpaEntity(School school, @MappingTarget SchoolJpaEntity schoolJpaEntity);

}
