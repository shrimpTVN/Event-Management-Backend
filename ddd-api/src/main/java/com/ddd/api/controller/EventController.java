package com.ddd.api.controller;

import com.ddd.application.service.event.EventCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor

public class EventController {
    private final EventCommandService eventCommandService;



}
