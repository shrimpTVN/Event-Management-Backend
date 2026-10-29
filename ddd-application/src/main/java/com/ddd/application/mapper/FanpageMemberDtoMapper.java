package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageMemberDtoMapper {

    @Mapping(source = "entity.user.id", target = "userId")
    @Mapping(source = "entity.user.email", target = "email")
    @Mapping(source = "entity.user.firstName", target = "firstName")
    @Mapping(source = "entity.user.lastName", target = "lastName")
    @Mapping(source = "entity.user.avatarUrl", target = "avatarUrl")
    FanpageMemberDto toDto(FanpageMemberJpaEntity entity);

    FanpageMemberJpaEntity toEntity(FanpageMemberDto dto);
}
