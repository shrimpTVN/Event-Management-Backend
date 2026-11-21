package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.req.fanpage.AlertAbsentStudentsRequestDto;
import com.ddd.api.dto.statistic.res.fanpage.*;
import com.ddd.api.mapper.FanpageStatisticApiMapper;
import com.ddd.application.service.fanpage.FanpageStatisticCommandService;
import com.ddd.application.service.fanpage.FanpageStatisticQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fanpages/{fanpageId}/statistics")
public class FanpageStatisticController {

    private final FanpageStatisticQueryService fanpageStatisticQueryService;
    private final FanpageStatisticCommandService fanpageStatisticCommandService;
    private final FanpageStatisticApiMapper fanpageStatisticApiMapper;

    @GetMapping("/overview")
    public BaseResponse<FanpageOverviewStatisticResponseDto> getOverview(@PathVariable Long fanpageId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toResponseDto(
                fanpageStatisticQueryService.getOverview(fanpageId)));
    }

    @GetMapping("/trends")
    public BaseResponse<List<FanpageMonthlyTrendResponseDto>> getMonthlyTrends(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toMonthlyTrendResponseList(
                fanpageStatisticQueryService.getMonthlyTrends(fanpageId, year, semesterId)));
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

    @GetMapping("/demographics")
    public BaseResponse<FanpageDemographicsResponseDto> getDemographics(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toResponseDto(
                fanpageStatisticQueryService.getDemographics(fanpageId, year, semesterId)));
    }

    @GetMapping("/dashboard")
    public BaseResponse<FanpageDashboardResponseDto> getDashboard(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toResponseDto(
                fanpageStatisticQueryService.getDashboard(fanpageId, year, semesterId)));
    }

    @GetMapping("/absent-rate")
    public BaseResponse<List<FanpageMonthlyAbsentRateResponseDto>> getMonthlyAbsentRate(
            @PathVariable Long fanpageId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId) {
        return BaseResponse.of(fanpageStatisticApiMapper.toMonthlyAbsentRateResponseList(
                fanpageStatisticQueryService.getMonthlyAbsentRate(fanpageId, year, semesterId)));
    }

    @GetMapping("/absent-students")
    public BaseResponse<List<FanpageStudentAbsenceResponseDto>> getStudentAbsenceCounts(
            @PathVariable Long fanpageId,
            @RequestParam Long semesterId,
            @RequestParam(required = false, defaultValue = "3") Integer minAbsences) {
        return BaseResponse.of(fanpageStatisticApiMapper.toStudentAbsenceResponseList(
                fanpageStatisticQueryService.getStudentAbsenceCounts(fanpageId, semesterId, minAbsences)));
    }

    @PostMapping("/absent-students/alert")
    public BaseResponse<Void> alertAbsentStudents(
            @PathVariable Long fanpageId,
            @RequestBody @Valid AlertAbsentStudentsRequestDto requestDto) {
        fanpageStatisticCommandService.alertAbsentStudents(
                fanpageId, fanpageStatisticApiMapper.toCommandDto(requestDto));
        return BaseResponse.ok();
    }
}
