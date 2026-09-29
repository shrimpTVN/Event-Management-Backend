package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.FanpageRegisterRequestDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.application.service.fanpage.FanpageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fanpages")
@RequiredArgsConstructor
public class FanpageController {
    private final FanpageService fanpageService;
    private final FanpageApiMapper fanpageApiMapper;

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
}
