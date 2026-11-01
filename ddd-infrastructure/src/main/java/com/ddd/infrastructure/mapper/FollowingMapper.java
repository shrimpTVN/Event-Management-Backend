package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.Following;
import com.ddd.infrastructure.entity.FollowingJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FollowingMapper {

    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(source = "isActive", target = "active")
    Following toDomain(FollowingJpaEntity entity);

    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(source = "active", target = "isActive")
    FollowingJpaEntity toJpaEntity(Following domain);
}
