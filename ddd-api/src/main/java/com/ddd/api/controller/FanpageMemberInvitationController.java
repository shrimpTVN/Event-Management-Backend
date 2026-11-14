package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.res.FanpageMemberInvitationResponseDto;
import com.ddd.api.mapper.FanpageApiMapper;
import com.ddd.application.dto.fanpage.FanpageMemberInvitationInfoDto;
import com.ddd.application.service.fanpage.FanpageMemberInvitationCommandService;
import com.ddd.application.service.fanpage.FanpageMemberInvitationQueryService;
import com.ddd.domain.enums.InvatitationStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fanpage-member-invitations")
@RequiredArgsConstructor
public class FanpageMemberInvitationController {
    private final FanpageMemberInvitationCommandService fanpageMemberInvitationCommandService;
    private final FanpageMemberInvitationQueryService fanpageMemberInvitationQueryService;
    private final FanpageApiMapper fanpageApiMapper;

    @GetMapping("/me")
    public BaseResponse<List<FanpageMemberInvitationResponseDto>> getAllFanpageMemberInvitationsForUser(
            Authentication authentication) {
        String email = authentication.getName();
        List<FanpageMemberInvitationInfoDto> invitations = fanpageMemberInvitationQueryService
                .getAllFanpageMemberInvitationsForUser(email);
        return BaseResponse.of(invitations.stream().map(fanpageApiMapper::toInvitationResponseDto).toList());
    }

    @PostMapping("/{invitationId}/accept")
    public BaseResponse<Void> acceptInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageMemberInvitationCommandService.acceptInvitation(invitationId, email);
        return BaseResponse.ok();
    }

    @PostMapping("/{invitationId}/decline")
    public BaseResponse<Void> declineInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageMemberInvitationCommandService.declineInvitation(invitationId, email);
        return BaseResponse.ok();
    }

    @PostMapping("/{invitationId}/cancel")
    public BaseResponse<Void> cancelInvitation(
            @PathVariable Long invitationId,
            Authentication authentication) {
        String email = authentication.getName();
        fanpageMemberInvitationCommandService.cancelInvitation(invitationId, email);
        return BaseResponse.ok();
    }

    @GetMapping("/fanpage/{fanpageId}")
    public BaseResponse<Page<FanpageMemberInvitationResponseDto>> getAllFanpageMemberInvitationsForFanpage(
            @PathVariable Long fanpageId,
            @RequestParam(defaultValue = "PENDING") InvatitationStatusEnum status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication) {
        String email = authentication.getName();
        Page<FanpageMemberInvitationInfoDto> invitations = fanpageMemberInvitationQueryService
                .getAllFanpageMemberInvitationsForFanpage(fanpageId, status, page, size, sortBy, sortDir, email);
        return BaseResponse.of(invitations.map(fanpageApiMapper::toInvitationResponseDto));
    }
}
