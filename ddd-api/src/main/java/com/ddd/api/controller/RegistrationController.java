package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.registration.res.RegistrationResponseDto;
import com.ddd.api.mapper.RegistrationApiMapper;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.registration.RegistrationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/me/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationCommandService registrationCommandService;
    private final RegistrationQueryService registrationQueryService;
    private final RegistrationApiMapper registrationApiMapper;

    @PostMapping("/events/{eventId}")
    public BaseResponse<Void> registerEvent(Authentication authentication,
                                            @PathVariable Long eventId) {
        String email = authentication.getName();
        registrationCommandService.registerEvent(email, eventId);
        return BaseResponse.ok();
    }


    @PatchMapping("/events/{eventId}/cancel")
    public BaseResponse<Void> cancelRegistration(Authentication authentication,
                                                 @PathVariable Long eventId,
                                                 @RequestBody Map<String, String> body) {
        String email = authentication.getName();
        String reason = body.getOrDefault("reason", "");

        registrationCommandService.cancelRegistration(email, eventId, reason);
        return BaseResponse.ok();
    }


    @GetMapping("/history")
    public BaseResponse<Page<RegistrationResponseDto>> getHistory(Authentication authentication,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        String email = authentication.getName();
        var summaryPage = registrationQueryService.getHistory(email, page, size);

        return BaseResponse.of(summaryPage.map(registrationApiMapper::toResponseDto));
    }


    @GetMapping("/upcoming")
    public BaseResponse<List<RegistrationResponseDto>> getUpcoming(Authentication authentication) {
        String email = authentication.getName();
        var summaryList = registrationQueryService.getUpcoming(email);

        List<RegistrationResponseDto> responseList = summaryList.stream()
                .map(registrationApiMapper::toResponseDto)
                .toList();

        return BaseResponse.of(responseList);
    }
}
