package com.ddd.application.service.admin.impl;

import com.ddd.application.dto.statistic.admin.*;
import com.ddd.application.mapper.AdminStatisticDtoMapper;
import com.ddd.domain.exception.ForbiddenException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.dto.row.admin.*;
import com.ddd.infrastructure.dto.row.fanpage.DemographicItemRow;
import com.ddd.infrastructure.dto.row.fanpage.MonthlyAbsentRateRow;
import com.ddd.infrastructure.repository.AdminStatisticRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminStatisticQueryServiceImplTest {

    @Mock
    private AdminStatisticRepositoryImpl adminStatisticRepositoryImpl;

    @Mock
    private SecurityContextPort securityContextPort;

    private AdminStatisticQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminStatisticQueryServiceImpl(
                adminStatisticRepositoryImpl,
                Mappers.getMapper(AdminStatisticDtoMapper.class),
                securityContextPort
        );
    }

    @Test
    @DisplayName("Should throw ForbiddenException when caller is not an ADMIN")
    void shouldThrowForbiddenException_whenNotAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> service.getOverview(2026, 1L, null));
        assertThrows(ForbiddenException.class, () -> service.getMonthlyTrends(2026, 1L, null));
        assertThrows(ForbiddenException.class, () -> service.getEventDistribution(2026, 1L, null));
        assertThrows(ForbiddenException.class, () -> service.getTopFanpages(2026, 1L, 10, null));
        assertThrows(ForbiddenException.class, () -> service.getDemographics(2026, 1L, null));
        assertThrows(ForbiddenException.class, () -> service.getMonthlyAbsentRate(2026, 1L, null));
        assertThrows(ForbiddenException.class, () -> service.getDashboard(2026, 1L, null));
    }

    @Test
    @DisplayName("Should return overview statistics when caller is an ADMIN")
    void shouldReturnOverview_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getUserOverview())
                .thenReturn(new AdminUserOverviewRow(100L, 80L, 10L, 95L));
        when(adminStatisticRepositoryImpl.getFanpageOverview())
                .thenReturn(new AdminFanpageOverviewRow(20L, 18L, 2L, 0L));
        when(adminStatisticRepositoryImpl.getEventOverview(2026, 1L, null))
                .thenReturn(new AdminEventOverviewRow(50L, 30L, 18L, 2L));
        when(adminStatisticRepositoryImpl.getAttendanceOverview(2026, 1L, null))
                .thenReturn(new AdminAttendanceOverviewRow(500L, 450L, 50L, 90.0, 10.0));

        AdminOverviewStatisticDto result = service.getOverview(2026, 1L, null);

        assertNotNull(result);
        assertEquals(100L, result.users().totalUsers());
        assertEquals(20L, result.fanpages().totalFanpages());
        assertEquals(50L, result.events().totalEvents());
        assertEquals(500L, result.attendance().totalRegistrations());
        assertEquals(90.0, result.attendance().attendanceRate());
    }

    @Test
    @DisplayName("Should return monthly trends when caller is an ADMIN")
    void shouldReturnMonthlyTrends_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getMonthlyTrends(2026, 1L, null))
                .thenReturn(List.of(new AdminMonthlyTrendRow("2026-03", 10L, 100L, 90L, 10L, 10.0)));

        List<AdminMonthlyTrendDto> result = service.getMonthlyTrends(2026, 1L, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2026-03", result.get(0).monthYear());
        assertEquals(10L, result.get(0).eventCount());
    }

    @Test
    @DisplayName("Should return event distribution when caller is an ADMIN")
    void shouldReturnEventDistribution_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getEventDistribution(2026, 1L, null))
                .thenReturn(List.of(
                        new AdminEventTypeDistributionRow(1L, "Academic", 30L),
                        new AdminEventTypeDistributionRow(2L, "Sports", 10L)
                ));

        List<AdminEventTypeDistributionDto> result = service.getEventDistribution(2026, 1L, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Academic", result.get(0).eventTypeName());
        assertEquals(75.0, result.get(0).percentage());
        assertEquals("Sports", result.get(1).eventTypeName());
        assertEquals(25.0, result.get(1).percentage());
    }

    @Test
    @DisplayName("Should return top fanpages when caller is an ADMIN")
    void shouldReturnTopFanpages_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getTopFanpages(2026, 1L, 10, null))
                .thenReturn(List.of(new AdminTopFanpageRow(1L, "Tech Club", "avatar.png", 15L, 200L, 500L)));

        List<AdminTopFanpageDto> result = service.getTopFanpages(2026, 1L, 10, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Tech Club", result.get(0).fanpageName());
        assertEquals(15L, result.get(0).eventCount());
    }

    @Test
    @DisplayName("Should return demographics when caller is an ADMIN")
    void shouldReturnDemographics_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getCohortDemographics(2026, 1L, null))
                .thenReturn(List.of(new DemographicItemRow("K18", 60L), new DemographicItemRow("K19", 40L)));
        when(adminStatisticRepositoryImpl.getSchoolDemographics(2026, 1L, null))
                .thenReturn(List.of(new DemographicItemRow("CS", 100L)));
        when(adminStatisticRepositoryImpl.getMajorDemographics(2026, 1L, null))
                .thenReturn(List.of(new DemographicItemRow("SE", 100L)));

        AdminDemographicsDto result = service.getDemographics(2026, 1L, null);

        assertNotNull(result);
        assertEquals(100L, result.totalRegistrations());
        assertEquals(2, result.cohorts().size());
        assertEquals(60.0, result.cohorts().get(0).percentage());
    }

    @Test
    @DisplayName("Should return monthly absent rate when caller is an ADMIN")
    void shouldReturnMonthlyAbsentRate_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getMonthlyAbsentRate(2026, 1L, null))
                .thenReturn(List.of(new MonthlyAbsentRateRow(3, "Mar", 100L, 15L, 15.0)));

        List<AdminMonthlyAbsentRateDto> result = service.getMonthlyAbsentRate(2026, 1L, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.get(0).month());
        assertEquals("Mar", result.get(0).monthName());
        assertEquals(15.0, result.get(0).absentRate());
    }

    @Test
    @DisplayName("Should return composite dashboard when caller is an ADMIN")
    void shouldReturnDashboard_whenAdmin() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        when(adminStatisticRepositoryImpl.getUserOverview())
                .thenReturn(new AdminUserOverviewRow(100L, 80L, 10L, 95L));
        when(adminStatisticRepositoryImpl.getFanpageOverview())
                .thenReturn(new AdminFanpageOverviewRow(20L, 18L, 2L, 0L));
        when(adminStatisticRepositoryImpl.getEventOverview(null, null, "2025-2026"))
                .thenReturn(new AdminEventOverviewRow(50L, 30L, 18L, 2L));
        when(adminStatisticRepositoryImpl.getAttendanceOverview(null, null, "2025-2026"))
                .thenReturn(new AdminAttendanceOverviewRow(500L, 450L, 50L, 90.0, 10.0));
        when(adminStatisticRepositoryImpl.getMonthlyTrends(null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getEventDistribution(null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getTopFanpages(null, null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getCohortDemographics(null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getSchoolDemographics(null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getMajorDemographics(null, null, "2025-2026"))
                .thenReturn(List.of());
        when(adminStatisticRepositoryImpl.getMonthlyAbsentRate(null, null, "2025-2026"))
                .thenReturn(List.of());

        AdminDashboardDto result = service.getDashboard(null, null, "2025-2026");

        assertNotNull(result);
        assertNotNull(result.overview());
        assertNotNull(result.monthlyTrends());
        assertNotNull(result.eventDistribution());
        assertNotNull(result.topFanpages());
        assertNotNull(result.demographics());
        assertNotNull(result.monthlyAbsentRates());
    }
}
