package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.fanpage.req.BatchAttendanceRequestDto;
import com.ddd.api.dto.registration.req.AttendanceRequestDto;
import com.ddd.api.dto.registration.res.AttendeeResponseDto;
import com.ddd.api.dto.registration.res.RegistrationArrangedResponseDto;
import com.ddd.api.dto.registration.res.RegistrationResponseDto;
import com.ddd.api.mapper.RegistrationApiMapper;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.registration.RegistrationQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    public BaseResponse<Void> registerEvent(@PathVariable Long eventId) {
        registrationCommandService.registerEvent(eventId);
        return BaseResponse.ok();
    }

    @PatchMapping("/me/events/{eventId}/cancel")
    public BaseResponse<Void> cancelRegistration(@PathVariable Long eventId,
                                                 @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");

        registrationCommandService.cancelRegistration(eventId, reason);
        return BaseResponse.ok();
    }


    @GetMapping("/me/history")
    public BaseResponse<Page<RegistrationResponseDto>> getHistory(@RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {

        var summaryPage = registrationQueryService.getHistory(page, size);
        return BaseResponse.of(summaryPage.map(registrationApiMapper::toResponseDto));
    }


    @GetMapping("/me/upcoming")
    public BaseResponse<List<RegistrationResponseDto>> getUpcoming() {
        var summaryList = registrationQueryService.getUpcoming();
        List<RegistrationResponseDto> responseList = summaryList.stream()
                .map(registrationApiMapper::toResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @GetMapping("/me/arranged")
    public BaseResponse<List<RegistrationArrangedResponseDto>> getArranged(@RequestParam Instant startTime,
                                                                           @RequestParam Instant endTime) {
        var summaryList = registrationQueryService.getArranged(startTime, endTime);
        List<RegistrationArrangedResponseDto> responseList = summaryList.stream()
                .map(registrationApiMapper::toArrangedResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @GetMapping(value = "/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportAttendeesCsv(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        byte[] csvBytes = registrationQueryService.exportAttendeesToCsv(fanpageId, eventId);
        String filename = "attendees-event-" + eventId + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }



    @PatchMapping("/me/events/{eventId}/check-in")
    public BaseResponse<Void> checkIn(@RequestBody(required = false) AttendanceRequestDto checkInRequestDto,
                                      @PathVariable Long eventId) {
        String proofUrl = checkInRequestDto != null ? checkInRequestDto.proofUrl() : null;
        registrationCommandService.checkIn(eventId, proofUrl);
        return BaseResponse.ok();
    }

    @PatchMapping("/me/events/{eventId}/check-out")
    public BaseResponse<Void> checkOut(@PathVariable Long eventId,
                                       @RequestBody(required = false) AttendanceRequestDto checkOutRequestDto) {
        String proofUrl = checkOutRequestDto != null ? checkOutRequestDto.proofUrl() : null;
        registrationCommandService.checkOut(eventId, proofUrl);
        return BaseResponse.ok();
    }

    @GetMapping("/fanpage-member/fanpages/{fanpageId}/events/{eventId}/attendees")
    public BaseResponse<List<AttendeeResponseDto>> getAttendeesOfEvent(
                                                                       @PathVariable Long eventId,
                                                                       @PathVariable Long fanpageId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(defaultValue = "regDate") String sortBy,
                                                                       @RequestParam(defaultValue = "desc") String sortDir) {
        Page<AttendeeInfoDto> attendees = registrationQueryService
                .getAttendeesOfEvent(fanpageId, eventId, page, size, sortBy, sortDir);
        return BaseResponse.of(attendees.stream().map(registrationApiMapper::toAttendeeResponseDto).toList());
    }

    @PatchMapping("/fanpage-member/events/{eventId}/check-in")
    public BaseResponse<Void> fanpageMemberBatchCheckIn(@PathVariable Long eventId,
                                                        @RequestBody @Valid BatchAttendanceRequestDto request) {
        registrationCommandService.fanpageMemberCheckIn(eventId, request.studentUserIds());
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpage-member/events/{eventId}/check-out")
    public BaseResponse<Void> fanpageMemberBatchCheckOut(@PathVariable Long eventId,
                                                         @RequestBody @Valid BatchAttendanceRequestDto request) {
        registrationCommandService.fanpageMemberCheckOut(eventId, request.studentUserIds());
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpage-member/events/{eventId}/absent")
    public BaseResponse<Void> fanpageMemberBatchAbsent(@PathVariable Long eventId,
                                                       @RequestBody @Valid BatchAttendanceRequestDto request) {
        registrationCommandService.fanpageMemberBatchAbsent(eventId, request.studentUserIds());
        return BaseResponse.ok();
    }
}
