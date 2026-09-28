package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.FanpageMember;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageMemberMapper {

    @Mapping(target = "fanpageId", source = "id.fanpageId")
    @Mapping(target = "userId", source = "id.userId")
    @Mapping(source = "isActive", target = "active")
    FanpageMember toDomain(FanpageMemberJpaEntity fanpageMember);

    @Mapping(target="id", ignore = true)
    @Mapping(source = "active", target = "isActive")
    FanpageMemberJpaEntity toJpaEntity(FanpageMember fanpageMember);
}
