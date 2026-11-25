package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.res.admin.*;
import com.ddd.api.mapper.AdminStatisticApiMapper;
import com.ddd.application.dto.statistic.admin.*;
import com.ddd.application.service.admin.AdminStatisticQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminStatisticControllerTest {

    @Mock
    private AdminStatisticQueryService adminStatisticQueryService;

    @Mock
    private AdminStatisticApiMapper adminStatisticApiMapper;

    @InjectMocks
    private AdminStatisticController controller;

    @Test
    @DisplayName("getOverview should return wrapped overview response")
    void getOverview() {
        AdminOverviewStatisticDto appDto = new AdminOverviewStatisticDto(
                new AdminUserOverviewDto(100L, 80L, 10L, 95L),
                new AdminFanpageOverviewDto(20L, 18L, 2L, 0L),
                new AdminEventOverviewDto(50L, 30L, 18L, 2L),
                new AdminAttendanceOverviewDto(500L, 450L, 50L, 90.0, 10.0)
        );
        AdminOverviewStatisticResponseDto resDto = new AdminOverviewStatisticResponseDto(
                new AdminUserOverviewResponseDto(100L, 80L, 10L, 95L),
                new AdminFanpageOverviewResponseDto(20L, 18L, 2L, 0L),
                new AdminEventOverviewResponseDto(50L, 30L, 18L, 2L),
                new AdminAttendanceOverviewResponseDto(500L, 450L, 50L, 90.0, 10.0)
        );

        when(adminStatisticQueryService.getOverview(2026, 1L, null)).thenReturn(appDto);
        when(adminStatisticApiMapper.toResponseDto(appDto)).thenReturn(resDto);

        BaseResponse<AdminOverviewStatisticResponseDto> response = controller.getOverview(2026, 1L, null);

        assertNotNull(response);
        assertEquals(BaseResponse.SUCCESS_REQUEST_MESSAGE, response.message());
        assertEquals(resDto, response.data());
        verify(adminStatisticQueryService).getOverview(2026, 1L, null);
    }

    @Test
    @DisplayName("getMonthlyTrends should return wrapped trends list")
    void getMonthlyTrends() {
        List<AdminMonthlyTrendDto> appList = List.of(new AdminMonthlyTrendDto("2026-03", 10L, 100L, 90L, 10L, 10.0));
        List<AdminMonthlyTrendResponseDto> resList = List.of(new AdminMonthlyTrendResponseDto("2026-03", 10L, 100L, 90L, 10L, 10.0));

        when(adminStatisticQueryService.getMonthlyTrends(2026, 1L, null)).thenReturn(appList);
        when(adminStatisticApiMapper.toMonthlyTrendResponseDtoList(appList)).thenReturn(resList);

        BaseResponse<List<AdminMonthlyTrendResponseDto>> response = controller.getMonthlyTrends(2026, 1L, null);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("2026-03", response.data().get(0).monthYear());
    }

    @Test
    @DisplayName("getEventDistribution should return wrapped event type distribution list")
    void getEventDistribution() {
        List<AdminEventTypeDistributionDto> appList = List.of(new AdminEventTypeDistributionDto(1L, "Academic", 30L, 75.0));
        List<AdminEventTypeDistributionResponseDto> resList = List.of(new AdminEventTypeDistributionResponseDto(1L, "Academic", 30L, 75.0));

        when(adminStatisticQueryService.getEventDistribution(2026, 1L, null)).thenReturn(appList);
        when(adminStatisticApiMapper.toEventTypeDistributionResponseDtoList(appList)).thenReturn(resList);

        BaseResponse<List<AdminEventTypeDistributionResponseDto>> response = controller.getEventDistribution(2026, 1L, null);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("Academic", response.data().get(0).eventTypeName());
    }

    @Test
    @DisplayName("getTopFanpages should return wrapped top fanpages list")
    void getTopFanpages() {
        List<AdminTopFanpageDto> appList = List.of(new AdminTopFanpageDto(1L, "Tech Club", "avatar.png", 15L, 200L, 500L));
        List<AdminTopFanpageResponseDto> resList = List.of(new AdminTopFanpageResponseDto(1L, "Tech Club", "avatar.png", 15L, 200L, 500L));

        when(adminStatisticQueryService.getTopFanpages(2026, 1L, 10, null)).thenReturn(appList);
        when(adminStatisticApiMapper.toTopFanpageResponseDtoList(appList)).thenReturn(resList);

        BaseResponse<List<AdminTopFanpageResponseDto>> response = controller.getTopFanpages(2026, 1L, 10, null);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("Tech Club", response.data().get(0).fanpageName());
    }

    @Test
    @DisplayName("getDemographics should return wrapped demographics response")
    void getDemographics() {
        AdminDemographicsDto appDto = new AdminDemographicsDto(
                100L,
                List.of(new AdminDemographicItemDto("K18", 60L, 60.0)),
                List.of(new AdminDemographicItemDto("CS", 100L, 100.0)),
                List.of(new AdminDemographicItemDto("SE", 100L, 100.0))
        );
        AdminDemographicsResponseDto resDto = new AdminDemographicsResponseDto(
                100L,
                List.of(new AdminDemographicItemResponseDto("K18", 60L, 60.0)),
                List.of(new AdminDemographicItemResponseDto("CS", 100L, 100.0)),
                List.of(new AdminDemographicItemResponseDto("SE", 100L, 100.0))
        );

        when(adminStatisticQueryService.getDemographics(2026, 1L, null)).thenReturn(appDto);
        when(adminStatisticApiMapper.toResponseDto(appDto)).thenReturn(resDto);

        BaseResponse<AdminDemographicsResponseDto> response = controller.getDemographics(2026, 1L, null);

        assertNotNull(response);
        assertEquals(100L, response.data().totalRegistrations());
        assertEquals(1, response.data().cohorts().size());
    }

    @Test
    @DisplayName("getMonthlyAbsentRate should return wrapped absent rate list")
    void getMonthlyAbsentRate() {
        List<AdminMonthlyAbsentRateDto> appList = List.of(new AdminMonthlyAbsentRateDto(3, "Mar", 100L, 15L, 15.0));
        List<AdminMonthlyAbsentRateResponseDto> resList = List.of(new AdminMonthlyAbsentRateResponseDto(3, "Mar", 100L, 15L, 15.0));

        when(adminStatisticQueryService.getMonthlyAbsentRate(2026, 1L, null)).thenReturn(appList);
        when(adminStatisticApiMapper.toMonthlyAbsentRateResponseDtoList(appList)).thenReturn(resList);

        BaseResponse<List<AdminMonthlyAbsentRateResponseDto>> response = controller.getMonthlyAbsentRate(2026, 1L, null);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals(15.0, response.data().get(0).absentRate());
    }

    @Test
    @DisplayName("getDashboard should return wrapped composite dashboard response")
    void getDashboard() {
        AdminDashboardDto appDto = new AdminDashboardDto(
                new AdminOverviewStatisticDto(
                        new AdminUserOverviewDto(100L, 80L, 10L, 95L),
                        new AdminFanpageOverviewDto(20L, 18L, 2L, 0L),
                        new AdminEventOverviewDto(50L, 30L, 18L, 2L),
                        new AdminAttendanceOverviewDto(500L, 450L, 50L, 90.0, 10.0)
                ),
                List.of(), List.of(), List.of(),
                new AdminDemographicsDto(0L, List.of(), List.of(), List.of()),
                List.of()
        );
        AdminDashboardResponseDto resDto = new AdminDashboardResponseDto(
                new AdminOverviewStatisticResponseDto(
                        new AdminUserOverviewResponseDto(100L, 80L, 10L, 95L),
                        new AdminFanpageOverviewResponseDto(20L, 18L, 2L, 0L),
                        new AdminEventOverviewResponseDto(50L, 30L, 18L, 2L),
                        new AdminAttendanceOverviewResponseDto(500L, 450L, 50L, 90.0, 10.0)
                ),
                List.of(), List.of(), List.of(),
                new AdminDemographicsResponseDto(0L, List.of(), List.of(), List.of()),
                List.of()
        );

        when(adminStatisticQueryService.getDashboard(null, null, "2025-2026")).thenReturn(appDto);
        when(adminStatisticApiMapper.toResponseDto(appDto)).thenReturn(resDto);

        BaseResponse<AdminDashboardResponseDto> response = controller.getDashboard(null, null, "2025-2026");

        assertNotNull(response);
        assertEquals(BaseResponse.SUCCESS_REQUEST_MESSAGE, response.message());
        assertEquals(resDto, response.data());
    }
}
