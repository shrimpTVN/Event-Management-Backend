package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.School;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SchoolMapper {
    School toDomainModel(SchoolJpaEntity schoolJpaEntity);
    SchoolJpaEntity toJpaEntity(School school);
}
