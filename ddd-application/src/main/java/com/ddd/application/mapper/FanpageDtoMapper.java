package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.*;
import com.ddd.domain.model.Fanpage;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FanpageDtoMapper {
    Fanpage toFanpage(FanpageRegisterDto registerDto);

    @Mapping(source = "active", target = "isActive")
    FanpageInfoDto toInfoDto(Fanpage fanpage);

    FanpageInfoDto toInfoDto(FanpageJpaEntity entity);

    @Mapping(target="fanpageId", source="fanpage.id")
    @Mapping(target="fanpageName", source="fanpage.name")
    @Mapping(target="fanpageDescription", source="fanpage.description")
    @Mapping(target="fanpageAvatarUrl", source="fanpage.avatarUrl")
    @Mapping(target="fanpageOrgName", source="fanpage.orgName")
    ManagedFanpageInfoDto toManagedInfoDto(FanpageMemberJpaEntity fanpageMemberJpaEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "orgType", ignore = true)
    @Mapping(target = "orgName", ignore = true)
    @Mapping(target = "parentOrg", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateFanpageFromAdminDto(FanpageAdminUpdateDto updateDto, @MappingTarget Fanpage fanpage);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", source = "isActive")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateFanpageFromSystemAdminDto(FanpageSystemAdminUpdateDto updateDto, @MappingTarget Fanpage fanpage);
}
