package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.FanpageAdminUpdateRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageFilterRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageRegisterRequestDto;
import com.ddd.api.dto.fanpage.req.FanpageSystemAdminUpdateRequestDto;
import com.ddd.api.dto.fanpage.res.FanpageFollowCountResponseDto;
import com.ddd.api.dto.fanpage.res.FanpageMemberResponseDto;
import com.ddd.api.dto.fanpage.res.FanpageResponseDto;
import com.ddd.api.dto.fanpage.res.ManagedFanpageResponseDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.application.dto.fanpage.FanpageFollowCountDto;
import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.ManagedFanpageInfoDto;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.utils.ServiceUtil;
import org.springframework.data.domain.Pageable;
import com.ddd.application.service.fanpage.FanpageCommandService;
import com.ddd.application.service.registration.RegistrationCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fanpages")
@RequiredArgsConstructor
public class FanpageController {
    private final FanpageCommandService fanpageCommandService;
    private final FanpageApiMapper fanpageApiMapper;
    private final RegistrationCommandService registrationCommandService;
    private final FanpageQueryService fanpageQueryService;

    @GetMapping({"", "/"})
    public BaseResponse<Page<FanpageResponseDto>> searchFanpages(
            @ModelAttribute FanpageFilterRequestDto request) {

        //set status to ACTIVE  and isActive to true for normal users,
        // only admin can see PENDING or BANNED fanpages
        FanpageFilterRequestDto effectiveFilter = new FanpageFilterRequestDto(
                request.keyword(),
                "ACTIVE",
                request.orgType(),
                true,
                request.page(),
                request.size(),
                request.sortBy(),
                request.sortDir()
        );
        Page<FanpageInfoDto> fanpages = fanpageQueryService.searchAndFilterFanpages(
                fanpageApiMapper.toSearchCriteriaDto(effectiveFilter)
        );
        return BaseResponse.of(fanpages.map(fanpageApiMapper::toResponseDto));
    }

    @GetMapping({"/admin", "/admin/"})
    public BaseResponse<Page<FanpageResponseDto>> searchFanpagesForAdmin(
            @ModelAttribute FanpageFilterRequestDto request) {
        Page<FanpageInfoDto> fanpages = fanpageQueryService.searchAndFilterFanpages(
                fanpageApiMapper.toSearchCriteriaDto(request)
        );
        return BaseResponse.of(fanpages.map(fanpageApiMapper::toResponseDto));
    }

    @GetMapping("/{fanpageId}")
    public BaseResponse<FanpageResponseDto> getFanpageDetail(@PathVariable Long fanpageId) {
        FanpageInfoDto fanpageInfo = fanpageQueryService.getFanpageById(fanpageId);
        return BaseResponse.of(fanpageApiMapper.toResponseDto(fanpageInfo));
    }

    @GetMapping({"/fanpage-admin/my-fanpages", "/my-fanpages"})
    public BaseResponse<List<ManagedFanpageResponseDto>> getMyManagedFanpages() {
        List<ManagedFanpageInfoDto> fanpages = fanpageQueryService.getMyManagedFanpages();
        return BaseResponse.of(fanpages.stream().map(fanpageApiMapper::toResponseDto).toList());
    }

    @PostMapping("/fanpage-admin/register")
    public BaseResponse<?> registerFanpage(@RequestBody @Valid FanpageRegisterRequestDto request,
                                           Authentication authentication) {
        String email = authentication.getName();
        fanpageCommandService.createFanpage(fanpageApiMapper.toRegisterDto(request), email);
        return BaseResponse.ok();
    }

    @PutMapping("/fanpage-admin/{fanpageId}")
    public BaseResponse<?> updateFanpageByFanpageAdmin(
            @PathVariable Long fanpageId,
            @RequestBody @Valid FanpageAdminUpdateRequestDto request,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageCommandService.updateFanpageByFanpageAdmin(fanpageId, fanpageApiMapper.toAdminUpdateDto(request), email);
        return BaseResponse.ok();
    }

    @PutMapping("/admin/{fanpageId}")
    public BaseResponse<?> updateFanpageByAdmin(
            @PathVariable Long fanpageId,
            @RequestBody @Valid FanpageSystemAdminUpdateRequestDto request) {
        fanpageCommandService.updateFanpageByAdmin(fanpageId, fanpageApiMapper.toSystemAdminUpdateDto(request));
        return BaseResponse.ok();
    }

    @DeleteMapping("/admin/{fanpageId}")
    public BaseResponse<?> deleteFanpageByAdmin(@PathVariable Long fanpageId) {
        fanpageCommandService.deleteFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @PostMapping("/admin/accept-fanpage/{fanpageId}")
    public BaseResponse<?> acceptFanpage(@PathVariable Long fanpageId) {
        fanpageCommandService.acceptFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @PostMapping("/admin/ban-fanpage/{fanpageId}")
    public BaseResponse<?> banFanpage(@PathVariable Long fanpageId) {
        fanpageCommandService.banFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @PostMapping("/admin/unban-fanpage/{fanpageId}")
    public BaseResponse<?> unbanFanpage(@PathVariable Long fanpageId) {
        fanpageCommandService.unbanFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @GetMapping("/{fanpageId}/members")
    public BaseResponse<List<FanpageMemberResponseDto>> getMembersOfFanpage(@PathVariable Long fanpageId) {

        List<FanpageMemberDto> members = fanpageQueryService.getMembersOfFanpage(fanpageId);
        return BaseResponse.of(members.stream().map(fanpageApiMapper::toMemberResponseDto).toList());
    }

    @PostMapping("/fanpage-admin/{fanpageId}/invite-member")
    public BaseResponse<?> inviteMemberToFanpage(@PathVariable Long fanpageId,
                                              @RequestParam(required = true) String email) {
        fanpageCommandService.inviteMemberToFanpage(fanpageId, email);
        return BaseResponse.ok();
    }

    @PostMapping("/fanpage-admin/{fanpageId}/remove-member")
    public BaseResponse<?> removeMemberFromFanpage(@PathVariable Long fanpageId,
                                                   @RequestParam (required = true) String email) {

        fanpageCommandService.removeMemberFromFanpage(fanpageId, email);
        return BaseResponse.ok();
    }


    @PatchMapping("/fanpage-admin/{fanpageId}/appoint-fanpage-admin")
    public BaseResponse<?> appointFanpageAdmin(@PathVariable Long fanpageId,
                                              @RequestParam(required = true) String email) {
        fanpageCommandService.appointFanpageAdmin(fanpageId, email);
        return BaseResponse.ok();
    }

    @PostMapping("/{fanpageId}/follow")
    public BaseResponse<Void> followFanpage(
            @PathVariable Long fanpageId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageCommandService.followFanpage(fanpageId, email);
        return BaseResponse.ok();
    }

    @DeleteMapping("/{fanpageId}/follow")
    public BaseResponse<Void> unfollowFanpage(
            @PathVariable Long fanpageId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageCommandService.unfollowFanpage(fanpageId, email);
        return BaseResponse.ok();
    }

    @PostMapping("/{fanpageId}/unfollow")
    public BaseResponse<Void> unfollowFanpageAlias(
            @PathVariable Long fanpageId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageCommandService.unfollowFanpage(fanpageId, email);
        return BaseResponse.ok();
    }

    @GetMapping("/{fanpageId}/followers/count")
    public BaseResponse<FanpageFollowCountResponseDto> getFollowerCount(@PathVariable Long fanpageId) {
        FanpageFollowCountDto dto = fanpageQueryService.getFollowerCount(fanpageId);
        return BaseResponse.of(fanpageApiMapper.toFollowCountResponseDto(dto));
    }

    @GetMapping("/following")
    public BaseResponse<Page<FanpageResponseDto>> getFollowingFanpages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<FanpageInfoDto> fanpages = fanpageQueryService.getFollowingFanpages(pageable);
        return BaseResponse.of(fanpages.map(fanpageApiMapper::toResponseDto));
    }

}
