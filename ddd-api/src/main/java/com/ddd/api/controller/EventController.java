package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.event.req.EventCreateRequestDto;
import com.ddd.api.dto.event.req.EventUpdateRequestDto;
import com.ddd.api.dto.event.res.EventResponseDto;
import com.ddd.api.mapper.EventApiMapper;
import com.ddd.application.dto.event.EventCreateDto;
import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.application.service.event.EventCommandService;
import com.ddd.application.service.event.EventQueryService;
import com.ddd.domain.enums.EventStatusEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCommandService eventCommandService;
    private final EventQueryService eventQueryService;
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

    @GetMapping("/{id}")
    public BaseResponse<EventResponseDto> getEventById(@PathVariable Long id) {
        EventInfoDto eventInfoDto = eventQueryService.getEventById(id);
        return BaseResponse.of(eventApiMapper.toResponseDto(eventInfoDto));
    }

    @GetMapping("/fanpage/{fanpageId}")
    public BaseResponse<Page<EventResponseDto>> getAllEventInfoByFanpageId(
            @PathVariable Long fanpageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Page<EventInfoDto> eventInfoPage = eventQueryService.getAllEventsByFanpageId(fanpageId, page, size, sortBy, sortDir);
        return BaseResponse.of(eventInfoPage.map(eventApiMapper::toResponseDto));
    }

    @PutMapping({"/fanpage-member/{id}"})
    public BaseResponse<Void> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateRequestDto eventUpdateRequestDto,
            Authentication authentication) {
        String email = authentication.getName();
        EventUpdateDto updateDto = eventApiMapper.toUpdateDto(eventUpdateRequestDto);
        eventCommandService.updateEvent(id, updateDto, email);
        return BaseResponse.ok();
    }

    @PatchMapping({"/fanpage-member/{id}/status"})
    public BaseResponse<Void> changeEventStatus(
            @PathVariable Long id,
            @RequestParam EventStatusEnum status,
            Authentication authentication) {
        String email = authentication.getName();
        eventCommandService.changeEventStatus(id, status, email);
        return BaseResponse.ok();
    }

    @DeleteMapping({"/{id}", "/fanpage-member/{id}"})
    public BaseResponse<Void> deleteEvent(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        eventCommandService.deleteEvent(id, email);
        return BaseResponse.ok();
    }
}

