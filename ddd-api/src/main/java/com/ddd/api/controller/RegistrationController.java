package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.BatchAttendanceRequestDto;
import com.ddd.api.dto.registration.req.AttendanceRequestDto;
import com.ddd.api.dto.registration.res.AttendeeResponseDto;
import com.ddd.api.dto.registration.res.RegistrationResponseDto;
import com.ddd.api.mapper.RegistrationApiMapper;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.registration.RegistrationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationCommandService registrationCommandService;
    private final RegistrationQueryService registrationQueryService;
    private final RegistrationApiMapper registrationApiMapper;

    @PostMapping("/me/events/{eventId}")
    public BaseResponse<Void> registerEvent(Authentication authentication,
                                            @PathVariable Long eventId) {
        String email = authentication.getName();
        registrationCommandService.registerEvent(email, eventId);
        return BaseResponse.ok();
    }

    @PatchMapping("/me/events/{eventId}/cancel")
    public BaseResponse<Void> cancelRegistration(Authentication authentication,
                                                 @PathVariable Long eventId,
                                                 @RequestBody Map<String, String> body) {
        String email = authentication.getName();
        String reason = body.getOrDefault("reason", "");

        registrationCommandService.cancelRegistration(email, eventId, reason);
        return BaseResponse.ok();
    }


    @GetMapping("/me/history")
    public BaseResponse<Page<RegistrationResponseDto>> getHistory(Authentication authentication,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        String email = authentication.getName();
        var summaryPage = registrationQueryService.getHistory(email, page, size);

        return BaseResponse.of(summaryPage.map(registrationApiMapper::toResponseDto));
    }


    @GetMapping("/me/upcoming")
    public BaseResponse<List<RegistrationResponseDto>> getUpcoming(Authentication authentication) {
        String email = authentication.getName();
        var summaryList = registrationQueryService.getUpcoming(email);

        List<RegistrationResponseDto> responseList = summaryList.stream()
                .map(registrationApiMapper::toResponseDto)
                .toList();

        return BaseResponse.of(responseList);
    }

    @PatchMapping("/me/events/{eventId}/check-in")
    public BaseResponse<Void> checkIn(Authentication authentication,
                                      @RequestBody(required = false) AttendanceRequestDto checkInRequestDto,
                                      @PathVariable Long eventId) {
        String email = authentication.getName();
        String proofUrl = checkInRequestDto != null ? checkInRequestDto.proofUrl() : null;
        registrationCommandService.checkIn(email, eventId, proofUrl);
        return BaseResponse.ok();
    }

    @PatchMapping("/me/events/{eventId}/check-out")
    public BaseResponse<Void> checkOut(Authentication authentication,
                                       @PathVariable Long eventId,
                                       @RequestBody(required = false) AttendanceRequestDto checkOutRequestDto) {
        String email = authentication.getName();
        String proofUrl = checkOutRequestDto != null ? checkOutRequestDto.proofUrl() : null;
        registrationCommandService.checkOut(email, eventId, proofUrl);
        return BaseResponse.ok();
    }

    @GetMapping("/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees")
    public BaseResponse<List<AttendeeResponseDto>> getAttendeesOfEvent(Authentication authentication,
                                                                       @PathVariable Long eventId,
                                                                       @PathVariable Long fanpageId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(defaultValue = "regDate") String sortBy,
                                                                       @RequestParam(defaultValue = "asc") String sortDir) {
        String fanpageMemberEmail = authentication.getName();
        Page<AttendeeInfoDto> attendees = registrationQueryService.getAttendeesOfEvent(fanpageMemberEmail, fanpageId, eventId, page, size, sortBy, sortDir);
        return BaseResponse.of(attendees.stream().map(registrationApiMapper::toAttendeeResponseDto).toList());
    }

    @PatchMapping("/fanpage-member/events/{eventId}/check-in")
    public BaseResponse<Void> fanpageMemberBatchCheckIn(Authentication authentication,
                                                        @PathVariable Long eventId,
                                                        @RequestBody @Valid BatchAttendanceRequestDto request) {
        String adminEmail = authentication.getName();
        registrationCommandService.fanpageMemberCheckIn(adminEmail, eventId, request.studentUserIds());
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpage-member/events/{eventId}/check-out")
    public BaseResponse<Void> fanpageMemberBatchCheckOut(Authentication authentication,
                                                         @PathVariable Long eventId,
                                                         @RequestBody @Valid BatchAttendanceRequestDto request) {
        String adminEmail = authentication.getName();
        registrationCommandService.fanpageMemberCheckOut(adminEmail, eventId, request.studentUserIds());
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpage-member/events/{eventId}/users/{studentUserId}/check-in")
    public BaseResponse<Void> fanpageMemberCheckIn(Authentication authentication,
                                                   @PathVariable Long eventId,
                                                   @PathVariable Long studentUserId) {
        String adminEmail = authentication.getName();
        registrationCommandService.fanpageMemberCheckIn(adminEmail, eventId, studentUserId);
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpage-member/events/{eventId}/users/{studentUserId}/check-out")
    public BaseResponse<Void> fanpageMemberCheckOut(Authentication authentication,
                                                    @PathVariable Long eventId,
                                                    @PathVariable Long studentUserId) {
        String adminEmail = authentication.getName();
        registrationCommandService.fanpageMemberCheckOut(adminEmail, eventId, studentUserId);
        return BaseResponse.ok();
    }

}
