package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.eventtype.req.CreateEventTypeRequestDto;
import com.ddd.api.dto.eventtype.req.UpdateEventTypeRequestDto;
import com.ddd.api.dto.eventtype.res.EventTypeResponseDto;
import com.ddd.api.mapper.EventTypeApiMapper;
import com.ddd.application.dto.eventtype.EventTypeDto;
import com.ddd.application.service.eventtype.EventTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event-types")
@RequiredArgsConstructor
public class EventTypeController {

    private final EventTypeService service;
    private final EventTypeApiMapper mapper;

    @GetMapping
    public BaseResponse<List<EventTypeResponseDto>> getAll() {
        List<EventTypeResponseDto> responses = service.getAllEventTypes().stream()
                .map(mapper::toResponseDto).toList();
        return BaseResponse.of(responses);
    }

    @GetMapping("/{id}")
    public BaseResponse<EventTypeResponseDto> getById(@PathVariable Long id) {
        return BaseResponse.of(mapper.toResponseDto(service.getEventTypeById(id)));
    }

    @PostMapping("/admin")
    public BaseResponse<EventTypeResponseDto> create(@RequestBody @Valid CreateEventTypeRequestDto req) {
        EventTypeDto dto = mapper.toDto(req);
        EventTypeDto created = service.createEventType(dto);
        return BaseResponse.of(mapper.toResponseDto(created));
    }

    @PutMapping("/admin/{id}")
    public BaseResponse<EventTypeResponseDto> update(@PathVariable Long id, @RequestBody @Valid UpdateEventTypeRequestDto req) {
        EventTypeDto dto = mapper.toDto(req);
        EventTypeDto updated = service.updateEventType(id, dto);
        return BaseResponse.of(mapper.toResponseDto(updated));
    }

    @DeleteMapping("/admin/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        service.deleteEventType(id);
        return BaseResponse.ok();
    }
}
