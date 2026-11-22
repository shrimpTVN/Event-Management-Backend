package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.res.admin.*;
import com.ddd.api.mapper.AdminStatisticApiMapper;
import com.ddd.application.service.admin.AdminStatisticQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/admin/statistics", "/api/admin/statistics"})
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticController {

    private final AdminStatisticQueryService adminStatisticQueryService;
    private final AdminStatisticApiMapper adminStatisticApiMapper;

    @GetMapping("/overview")
    public BaseResponse<AdminOverviewStatisticResponseDto> getOverview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toResponseDto(
                adminStatisticQueryService.getOverview(year, semesterId)
        ));
    }

    @GetMapping("/trends")
    public BaseResponse<List<AdminMonthlyTrendResponseDto>> getMonthlyTrends(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toMonthlyTrendResponseDtoList(
                adminStatisticQueryService.getMonthlyTrends(year, semesterId)
        ));
    }

    @GetMapping("/event-distribution")
    public BaseResponse<List<AdminEventTypeDistributionResponseDto>> getEventDistribution(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toEventTypeDistributionResponseDtoList(
                adminStatisticQueryService.getEventDistribution(year, semesterId)
        ));
    }

    @GetMapping("/top-fanpages")
    public BaseResponse<List<AdminTopFanpageResponseDto>> getTopFanpages(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toTopFanpageResponseDtoList(
                adminStatisticQueryService.getTopFanpages(year, semesterId, limit)
        ));
    }

    @GetMapping("/demographics")
    public BaseResponse<AdminDemographicsResponseDto> getDemographics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toResponseDto(
                adminStatisticQueryService.getDemographics(year, semesterId)
        ));
    }

    @GetMapping("/absent-rate")
    public BaseResponse<List<AdminMonthlyAbsentRateResponseDto>> getMonthlyAbsentRate(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toMonthlyAbsentRateResponseDtoList(
                adminStatisticQueryService.getMonthlyAbsentRate(year, semesterId)
        ));
    }

    @GetMapping("/dashboard")
    public BaseResponse<AdminDashboardResponseDto> getDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long semesterId
    ) {
        return BaseResponse.of(adminStatisticApiMapper.toResponseDto(
                adminStatisticQueryService.getDashboard(year, semesterId)
        ));
    }
}
