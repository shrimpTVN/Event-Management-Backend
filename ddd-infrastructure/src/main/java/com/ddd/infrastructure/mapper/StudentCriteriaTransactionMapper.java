package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StudentCriteriaTransaction;
import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentCriteriaTransactionMapper {

    @Mapping(target = "isActive", source = "active")
    StudentCriteriaTransactionJpaEntity toEntity(StudentCriteriaTransaction transaction);

    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "criteriaId", source = "criteria.id")
    @Mapping(target = "academicYearId", source = "academicYear.id")
    @Mapping(target = "eventId", source = "event.id")
    StudentCriteriaTransaction toDomain(StudentCriteriaTransactionJpaEntity entity);
}
