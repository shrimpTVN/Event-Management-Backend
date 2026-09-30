package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.api.mapper.EventApiMapper;
import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.service.event.EventCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCommandService eventCommandService;
    private final EventApiMapper eventApiMapper;

    @PostMapping("/fanpage-member")
    public BaseResponse<?> createEvent(
            @Valid @RequestBody EventCreateRequestDto eventCreateRequestDto,
            Authentication authentication) {
        String email = authentication.getName();
        EventCreateDto createDto = eventApiMapper.toCreateDto(eventCreateRequestDto);
        eventCommandService.createEvent(createDto, email);
        return BaseResponse.ok();
    }
}

