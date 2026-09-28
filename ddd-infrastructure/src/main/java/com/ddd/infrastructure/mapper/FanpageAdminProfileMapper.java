package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.FanpageAdminProfile;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface FanpageAdminProfileMapper {

    @Mapping(target="userId", source="user.id")
    FanpageAdminProfile toDomain(FanpageAdminProfileJpaEntity fanpageAdminProfileJpaEntity);

    @Mapping(target="user", ignore = true)
    FanpageAdminProfileJpaEntity toJpaEntity(FanpageAdminProfile fanpageAdminProfile);
}
