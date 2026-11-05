package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentCriteriaProgressMapper {
    @Mapping(source = "isActive", target = "active")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "criteria.id", target = "criteriaId")
    @Mapping(source = "academicYear.id", target = "academicYearId")
    StudentCriteriaProgress toDomain(StudentCriteriaProgressJpaEntity entity);

    @Mapping(source = "active", target = "isActive")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "criteria", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    StudentCriteriaProgressJpaEntity toJpaEntity(StudentCriteriaProgress domain);

    List<StudentCriteriaProgress> toDomainList(List<StudentCriteriaProgressJpaEntity> entities);

    List<StudentCriteriaProgressJpaEntity> toJpaEntityList(List<StudentCriteriaProgress> domains);
}
