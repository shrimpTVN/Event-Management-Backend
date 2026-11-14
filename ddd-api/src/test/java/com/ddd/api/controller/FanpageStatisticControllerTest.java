package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.statistic.res.fanpage.*;
import com.ddd.api.mapper.FanpageStatisticApiMapper;
import com.ddd.application.dto.statistic.*;
import com.ddd.application.service.fanpage.FanpageStatisticQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FanpageStatisticControllerTest {

    @Mock
    private FanpageStatisticQueryService fanpageStatisticQueryService;

    @Mock
    private FanpageStatisticApiMapper fanpageStatisticApiMapper;

    @InjectMocks
    private FanpageStatisticController controller;

    private final Long fanpageId = 1L;

    @Test
    @DisplayName("getOverview should return wrapped overview response")
    void getOverview() {
        FanpageOverviewStatisticDto appDto = new FanpageOverviewStatisticDto(5, 20, 3, 100, 80, 80.0);
        FanpageOverviewStatisticResponseDto resDto = new FanpageOverviewStatisticResponseDto(5, 20, 3, 100, 80, 80.0);

        when(fanpageStatisticQueryService.getOverview(fanpageId)).thenReturn(appDto);
        when(fanpageStatisticApiMapper.toResponseDto(appDto)).thenReturn(resDto);

        BaseResponse<FanpageOverviewStatisticResponseDto> response = controller.getOverview(fanpageId);

        assertNotNull(response);
        assertEquals(BaseResponse.SUCCESS_REQUEST_MESSAGE, response.message());
        assertEquals(resDto, response.data());
        verify(fanpageStatisticQueryService).getOverview(fanpageId);
    }

    @Test
    @DisplayName("getMonthlyTrends should return wrapped trends list")
    void getMonthlyTrends() {
        List<FanpageMonthlyTrendDto> appList = List.of(new FanpageMonthlyTrendDto("2026-08", 2, 50, 40));
        List<FanpageMonthlyTrendResponseDto> resList = List.of(new FanpageMonthlyTrendResponseDto("2026-08", 2, 50, 40));

        when(fanpageStatisticQueryService.getMonthlyTrends(fanpageId, 2026)).thenReturn(appList);
        when(fanpageStatisticApiMapper.toMonthlyTrendResponseList(appList)).thenReturn(resList);

        BaseResponse<List<FanpageMonthlyTrendResponseDto>> response = controller.getMonthlyTrends(fanpageId, 2026);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("2026-08", response.data().get(0).monthYear());
    }

    @Test
    @DisplayName("getEventDistribution should return wrapped event type distribution list")
    void getEventDistribution() {
        List<FanpageEventDistributionDto> appList = List.of(new FanpageEventDistributionDto("Workshop", 3, 60.0));
        List<FanpageEventDistributionResponseDto> resList = List.of(new FanpageEventDistributionResponseDto("Workshop", 3, 60.0));

        when(fanpageStatisticQueryService.getEventDistribution(fanpageId)).thenReturn(appList);
        when(fanpageStatisticApiMapper.toDistributionResponseList(appList)).thenReturn(resList);

        BaseResponse<List<FanpageEventDistributionResponseDto>> response = controller.getEventDistribution(fanpageId);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("Workshop", response.data().get(0).eventTypeName());
    }

    @Test
    @DisplayName("getTopEvents should return wrapped top events list")
    void getTopEvents() {
        Instant now = Instant.now();
        List<FanpageTopEventDto> appList = List.of(new FanpageTopEventDto(10L, "AI Talk", "banner.png", now, 100, 100, 95, 95.0));
        List<FanpageTopEventResponseDto> resList = List.of(new FanpageTopEventResponseDto(10L, "AI Talk", "banner.png", now, 100, 100, 95, 95.0));

        when(fanpageStatisticQueryService.getTopEvents(fanpageId, 5)).thenReturn(appList);
        when(fanpageStatisticApiMapper.toTopEventResponseList(appList)).thenReturn(resList);

        BaseResponse<List<FanpageTopEventResponseDto>> response = controller.getTopEvents(fanpageId, 5);

        assertNotNull(response);
        assertEquals(1, response.data().size());
        assertEquals("AI Talk", response.data().get(0).eventName());
    }

    @Test
    @DisplayName("getDashboard should return wrapped composite dashboard")
    void getDashboard() {
        FanpageDashboardDto appDto = new FanpageDashboardDto(
                new FanpageOverviewStatisticDto(1, 10, 2, 50, 40, 80.0),
                List.of(), List.of(), List.of()
        );
        FanpageDashboardResponseDto resDto = new FanpageDashboardResponseDto(
                new FanpageOverviewStatisticResponseDto(1, 10, 2, 50, 40, 80.0),
                List.of(), List.of(), List.of()
        );

        when(fanpageStatisticQueryService.getDashboard(fanpageId, 2026, 1L)).thenReturn(appDto);
        when(fanpageStatisticApiMapper.toResponseDto(appDto)).thenReturn(resDto);

        BaseResponse<FanpageDashboardResponseDto> response = controller.getDashboard(fanpageId, 2026, 1L);

        assertNotNull(response);
        assertEquals(BaseResponse.SUCCESS_REQUEST_MESSAGE, response.message());
        assertEquals(resDto, response.data());
    }
}
