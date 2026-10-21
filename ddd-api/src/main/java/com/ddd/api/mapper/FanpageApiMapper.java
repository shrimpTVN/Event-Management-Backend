package com.ddd.api.mapper;

import com.ddd.api.dto.fanpage.req.FanpageAdminUpdateRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageFilterRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageRegisterRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageSystemAdminUpdateRequestDto;
import com.ddd.api.dto.fanpage.res.FanpageFollowCountResponseDto;
import com.ddd.api.dto.fanpage.res.FanpageMemberResponseDto;
import com.ddd.api.dto.fanpage.res.FanpageResponseDto;
import com.ddd.api.dto.registration.res.AttendeeResponseDto;
import com.ddd.application.dto.fanpage.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FanpageApiMapper {
    FanpageResponseDto toResponseDto(FanpageInfoDto fanpageInfoDto);
    FanpageRegisterDto toRegisterDto(FanpageRegisterRequestDto requestDto);

    FanpageMemberResponseDto toMemberResponseDto(FanpageMemberDto memberDto);

    FanpageSearchCriteriaDto toSearchCriteriaDto(FanpageFilterRequestDto requestDto);

    FanpageAdminUpdateDto toAdminUpdateDto(FanpageAdminUpdateRequestDto requestDto);

    FanpageSystemAdminUpdateDto toSystemAdminUpdateDto(FanpageSystemAdminUpdateRequestDto requestDto);

    FanpageFollowCountResponseDto toFollowCountResponseDto(FanpageFollowCountDto dto);
}
