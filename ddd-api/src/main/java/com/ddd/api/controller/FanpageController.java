package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.FanpageRegisterRequestDto;
import com.ddd.api.dto.fanpage.res.FanpageMemberResponseDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.service.fanpage.FanpageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fanpages")
@RequiredArgsConstructor
public class FanpageController {
    private final FanpageService fanpageService;
    private final FanpageApiMapper fanpageApiMapper;
    private final FanpageQueryService fanpageQueryService;

    @PostMapping("/fanpage-admin/register")
    public BaseResponse<?> registerFanpage(@RequestBody @Valid FanpageRegisterRequestDto request,
                                           Authentication authentication) {
        String email = authentication.getName();
        fanpageService.createFanpage(fanpageApiMapper.toRegisterDto(request), email);
        return BaseResponse.ok();
    }

    @PostMapping("/admin/accept-fanpage/{fanpageId}")
    public BaseResponse<?> acceptFanpage(@PathVariable Long fanpageId) {
        fanpageService.acceptFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @PostMapping("/admin/ban-fanpage/{fanpageId}")
    public BaseResponse<?> banFanpage(@PathVariable Long fanpageId) {
        fanpageService.banFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @GetMapping("/{fanpageId}/members")
    public BaseResponse<List<FanpageMemberResponseDto>> getMembersOfFanpage(@PathVariable Long fanpageId) {

        List<FanpageMemberDto> members = fanpageQueryService.getMembersOfFanpage(fanpageId);
        return BaseResponse.of(members.stream().map(fanpageApiMapper::toMemberResponseDto).toList());
    }

    @PostMapping("/fanpage-admin/{fanpageId}/add-member/{userId}")
    public BaseResponse<?> addMemberToFanpage(@PathVariable Long fanpageId,
                                              @PathVariable Long userId, Authentication authentication) {
        String fanpageAdminEmail = authentication.getName();
        fanpageService.addMemberToFanpage(fanpageId, userId, fanpageAdminEmail);
        return BaseResponse.ok();
    }

    @PostMapping("/fanpage-admin/{fanpageId}/remove-member/{userId}")
    public BaseResponse<?> removeMemberFromFanpage(@PathVariable Long fanpageId,
                                                   @PathVariable Long userId, Authentication authentication) {
        String email = authentication.getName();
        fanpageService.removeMemberFromFanpage(fanpageId, userId, email);
        return BaseResponse.ok();
    }

}
