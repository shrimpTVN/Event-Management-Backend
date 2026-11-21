package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.res.event.*;
import com.ddd.api.mapper.EventStatisticApiMapper;
import com.ddd.application.service.event.EventStatisticQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fanpages/{fanpageId}/events/{eventId}/statistics")
public class EventStatisticController {
    private final EventStatisticQueryService eventStatisticQueryService;
    private final EventStatisticApiMapper eventStatisticApiMapper;

    @GetMapping("/overview")
    public BaseResponse<EventOverviewStatisticResponseDto> getOverview(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        return BaseResponse.of(eventStatisticApiMapper.toResponseDto(
                eventStatisticQueryService.getOverview(fanpageId, eventId)));
    }

    @GetMapping("/status-funnel")
    public BaseResponse<EventStatusFunnelResponseDto> getStatusFunnel(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        return BaseResponse.of(eventStatisticApiMapper.toResponseDto(
                eventStatisticQueryService.getStatusFunnel(fanpageId, eventId)));
    }

    @GetMapping("/demographics")
    public BaseResponse<EventDemographicsResponseDto> getDemographics(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        return BaseResponse.of(eventStatisticApiMapper.toResponseDto(
                eventStatisticQueryService.getDemographics(fanpageId, eventId)));
    }

    @GetMapping("/timeline")
    public BaseResponse<List<EventCheckInTimelineResponseDto>> getTimeline(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        return BaseResponse.of(eventStatisticApiMapper.toTimelineResponseList(
                eventStatisticQueryService.getCheckInTimeline(fanpageId, eventId)));
    }

    @GetMapping({"", "/dashboard"})
    public BaseResponse<EventDashboardStatisticResponseDto> getDashboard(
            @PathVariable Long fanpageId,
            @PathVariable Long eventId) {
        return BaseResponse.of(eventStatisticApiMapper.toResponseDto(
                eventStatisticQueryService.getDashboard(fanpageId, eventId)));
    }
}
