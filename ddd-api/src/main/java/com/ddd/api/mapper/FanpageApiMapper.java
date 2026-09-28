package com.ddd.api.mapper;

import com.ddd.api.dto.fanpage.req.FanpageRegisterRequestDto;
import com.ddd.api.dto.fanpage.res.FanpageResponseDto;
import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FanpageApiMapper {
    FanpageResponseDto toResponseDto(FanpageInfoDto fanpageInfoDto);
    FanpageRegisterDto toRegisterDto(FanpageRegisterRequestDto requestDto);


}
