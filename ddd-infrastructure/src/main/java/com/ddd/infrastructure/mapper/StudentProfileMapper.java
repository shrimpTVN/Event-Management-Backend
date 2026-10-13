package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.StudentProfile;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentProfileMapper {


    @Mapping(target="user", ignore = true)
    @Mapping(target="major", ignore = true)
    StudentProfileJpaEntity toEntity(StudentProfile studentProfile);

    @Mapping(target="userId", source = "user.id")
    @Mapping(target="majorId", source = "major.id")
    StudentProfile toDomain(StudentProfileJpaEntity studentProfileJpaEntity);
}
