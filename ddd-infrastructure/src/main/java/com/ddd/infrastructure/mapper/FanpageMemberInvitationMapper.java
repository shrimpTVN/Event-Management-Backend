package com.ddd.infrastructure.mapper;

import com.ddd.domain.model.FanpageMemberInvitation;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface FanpageMemberInvitationMapper {
    @Mapping(target = "fanpageId", source = "fanpage.id")
    @Mapping(target = "userId", source = "user.id")
    FanpageMemberInvitation toDomain(FanpageMemberInvitationJpaEntity fanpageMemberInvitationJpaEntity);

    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "user", ignore = true)
    FanpageMemberInvitationJpaEntity toJpaEntity(FanpageMemberInvitation fanpageMemberInvitation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fanpage", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateJpaEntityFromDomain(FanpageMemberInvitation domain, @MappingTarget FanpageMemberInvitationJpaEntity entity);
}
