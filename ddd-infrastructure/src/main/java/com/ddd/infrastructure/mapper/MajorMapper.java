package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Major;
import com.ddd.infrastructure.entity.MajorJpaEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface MajorMapper {

    @Mapping(source = "school.id", target = "schoolId")
    @Mapping(source = "isActive", target = "active")
    Major toDomainModel(MajorJpaEntity entity);

    @Mapping(source = "schoolId", target = "school.id")
    @Mapping(source = "active", target = "isActive")
    MajorJpaEntity toJpaEntity(Major domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "schoolId", target = "school.id")
    @Mapping(source = "active", target = "isActive")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDomain(Major major, @MappingTarget MajorJpaEntity entity);
}
