package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.domain.model.Fanpage;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FanpageDtoMapper {
    Fanpage toFanpage(FanpageRegisterDto registerDto);

    @Mapping(source = "active", target = "isActive")
    FanpageInfoDto toInfoDto(Fanpage fanpage);

    FanpageInfoDto toInfoDto(FanpageJpaEntity entity);
}
