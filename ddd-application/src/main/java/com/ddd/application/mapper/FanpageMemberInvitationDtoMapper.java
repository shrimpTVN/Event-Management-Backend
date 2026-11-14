package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.FanpageMemberInvitationInfoDto;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageMemberInvitationDtoMapper {

    @Mapping(source = "fanpage.id", target = "fanpageId")
    @Mapping(source = "fanpage.name", target = "fanpageName")
    @Mapping(source = "fanpage.avatarUrl", target = "fanpageAvatarUrl")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "user.firstName", target = "userFirstName")
    @Mapping(source = "user.lastName", target = "userLastName")
    @Mapping(source = "user.avatarUrl", target = "userAvatarUrl")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "createdAt", target = "createdAt")
    FanpageMemberInvitationInfoDto toInfoDto(FanpageMemberInvitationJpaEntity entity);
}
