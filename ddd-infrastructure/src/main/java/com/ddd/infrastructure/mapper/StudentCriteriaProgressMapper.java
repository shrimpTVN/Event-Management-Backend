package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentCriteriaProgressMapper {
    @Mapping(source = "isActive", target = "active")
    StudentCriteriaProgress toDomain(StudentCriteriaProgressJpaEntity entity);

    @Mapping(source = "active", target = "isActive")
    StudentCriteriaProgressJpaEntity toJpaEntity(StudentCriteriaProgress domain);

    List<StudentCriteriaProgress> toDomainList(List<StudentCriteriaProgressJpaEntity> entities);

    List<StudentCriteriaProgressJpaEntity> toJpaEntityList(List<StudentCriteriaProgress> domains);
}
