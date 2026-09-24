package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Semester;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SemesterMapper {

    Semester toDomain(SemesterJpaEntity entity);
    SemesterJpaEntity toJpaEntity(Semester entity);
}
