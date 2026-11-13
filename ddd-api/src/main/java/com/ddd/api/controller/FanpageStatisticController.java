package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.res.*;
import com.ddd.api.mapper.FanpageStatisticApiMapper;
import com.ddd.application.service.fanpage.FanpageStatisticQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fanpages/{fanpageId}/statistics")
public class FanpageStatisticController {

    private final FanpageStatisticQueryService fanpageStatisticQueryService;
    private final FanpageStatisticApiMapper fanpageStatisticApiMapper;

    @GetMapping("/overview")
    public BaseResponse<FanpageOverviewStatisticResponseDto> getOverview(@PathVariable Long fanpageId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toResponseDto(
                fanpageStatisticQueryService.getOverview(fanpageId)));
    }

    @GetMapping("/trends")
    public BaseResponse<List<FanpageMonthlyTrendResponseDto>> getMonthlyTrends(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year) {
        return BaseResponse.of(fanpageStatisticApiMapper.toMonthlyTrendResponseList(
                fanpageStatisticQueryService.getMonthlyTrends(fanpageId, year)));
    }

    @GetMapping("/event-distribution")
    public BaseResponse<List<FanpageEventDistributionResponseDto>> getEventDistribution(
            @PathVariable Long fanpageId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toDistributionResponseList(
                fanpageStatisticQueryService.getEventDistribution(fanpageId)));
    }

    @GetMapping("/top-events")
    public BaseResponse<List<FanpageTopEventResponseDto>> getTopEvents(
            @PathVariable Long fanpageId,
            @RequestParam(defaultValue = "5") int limit) {
        return BaseResponse.of(fanpageStatisticApiMapper.toTopEventResponseList(
                fanpageStatisticQueryService.getTopEvents(fanpageId, limit)));
    }

    @GetMapping("/dashboard")
    public BaseResponse<FanpageDashboardResponseDto> getDashboard(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year) {
        return BaseResponse.of(fanpageStatisticApiMapper.toResponseDto(
                fanpageStatisticQueryService.getDashboard(fanpageId, year)));
    }
}
