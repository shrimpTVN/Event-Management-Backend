package com.ddd.application.mapper;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.domain.model.Fanpage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FanpageDtoMapper {
    Fanpage toFanpage(FanpageRegisterDto registerDto);

    FanpageInfoDto toInfoDto(Fanpage fanpage);
}
