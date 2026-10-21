package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.AcademicYear;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AcademicYearMapper {
    @Mapping(source = "isActive", target = "active")
    AcademicYear toDomain(AcademicYearJpaEntity entity);

    @Mapping(source = "active", target = "isActive")
    AcademicYearJpaEntity toJpaEntity(AcademicYear domain);

    List<AcademicYear> toDomainList(List<AcademicYearJpaEntity> entities);
}
