package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.FanpageMember;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageDtoMapper {
    Fanpage toFanpage(FanpageRegisterDto registerDto);

    FanpageInfoDto toInfoDto(Fanpage fanpage);

    @Mapping(target = "userId", source = "fanpageMemberJpaEntity.user.id")
    @Mapping(target = "email", source = "fanpageMemberJpaEntity.user.email")
    FanpageMemberDto toMemberDto(FanpageMemberJpaEntity fanpageMemberJpaEntity);
}
