package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.statistic.*;
import com.ddd.application.mapper.FanpageStatisticDtoMapper;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.exception.ForbiddenException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FanpageStatisticRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.repository.FanpageStatisticRepositoryImpl;
import com.ddd.infrastructure.dto.row.fanpage.EventTypeDistributionRow;
import com.ddd.infrastructure.dto.row.fanpage.FanpageOverviewRow;
import com.ddd.infrastructure.dto.row.fanpage.MonthlyAbsentRateRow;
import com.ddd.infrastructure.dto.row.fanpage.MonthlyTrendRow;
import com.ddd.infrastructure.dto.row.fanpage.StudentAbsenceRow;
import com.ddd.infrastructure.dto.row.fanpage.TopEventRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FanpageStatisticQueryServiceImplTest {

    @Mock
    private FanpageStatisticRepository fanpageStatisticRepository;

    @Mock
    private FanpageStatisticRepositoryImpl fanpageStatisticRepositoryImpl;

    @Mock
    private FollowingRepository followingRepository;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @Mock
    private FanpageRepository fanpageRepository;

    @Mock
    private SecurityContextPort securityContextPort;

    private FanpageStatisticQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FanpageStatisticQueryServiceImpl(
                fanpageStatisticRepository,
                fanpageStatisticRepositoryImpl,
                followingRepository,
                fanpageMemberRepository,
                fanpageRepository,
                securityContextPort,
                Mappers.getMapper(FanpageStatisticDtoMapper.class)
        );
    }

    private final Long fanpageId = 1L;
    private final Long userId = 100L;

    @Nested
    @DisplayName("Authorization and Guard Tests")
    class GuardTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when fanpage does not exist")
        void shouldThrowWhenFanpageNotFound() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.getOverview(fanpageId));
            verify(fanpageRepository).findByIdAndIsActive(fanpageId, true);
            verifyNoInteractions(securityContextPort);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not authenticated")
        void shouldThrowWhenNotAuthenticated() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(null);

            assertThrows(UnauthorizedException.class, () -> service.getOverview(fanpageId));
        }

        @Test
        @DisplayName("Should throw AuthException when user is not admin of fanpage and not system admin")
        void shouldThrowWhenNotAdmin() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(false);
            when(fanpageMemberRepository.isActiveMember(fanpageId, userId)).thenReturn(false);

            assertThrows(ForbiddenException.class, () -> service.getOverview(fanpageId));
        }

        @Test
        @DisplayName("Should permit access when caller is system admin")
        void shouldPermitSystemAdmin() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
            when(fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId))
                    .thenReturn(new FanpageOverviewRow(10, 50, 5, 100, 80));
            when(followingRepository.countByFanpageIdAndFolStatus(fanpageId, FollowingStatusEnum.FOLLOWING))
                    .thenReturn(50L);

            FanpageOverviewStatisticDto overview = service.getOverview(fanpageId);

            assertNotNull(overview);
            assertEquals(10, overview.totalEvents());
            assertEquals(80.0, overview.attendanceRate());
            verify(fanpageMemberRepository, never()).isActiveMember(anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("Metric Calculation Tests")
    class MetricCalculationTests {

        @Test
        @DisplayName("Should calculate overview metrics correctly including attendance rate")
        void shouldCalculateOverview() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(false);
            when(fanpageMemberRepository.isActiveMember(fanpageId, userId)).thenReturn(true);

            when(fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId))
                    .thenReturn(new FanpageOverviewRow(5, 20, 3, 200, 150));
            when(followingRepository.countByFanpageIdAndFolStatus(fanpageId, FollowingStatusEnum.FOLLOWING))
                    .thenReturn(20L);

            FanpageOverviewStatisticDto overview = service.getOverview(fanpageId);

            assertEquals(5, overview.totalEvents());
            assertEquals(20, overview.totalFollowers());
            assertEquals(3, overview.totalMembers());
            assertEquals(200, overview.totalRegistrations());
            assertEquals(150, overview.totalAttendees());
            assertEquals(75.0, overview.attendanceRate());
        }

        @Test
        @DisplayName("Should handle zero registrations without division by zero")
        void shouldHandleZeroRegistrations() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            when(fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId))
                    .thenReturn(new FanpageOverviewRow(1, 0, 1, 0, 0));
            when(followingRepository.countByFanpageIdAndFolStatus(fanpageId, FollowingStatusEnum.FOLLOWING))
                    .thenReturn(0L);

            FanpageOverviewStatisticDto overview = service.getOverview(fanpageId);

            assertEquals(0, overview.totalRegistrations());
            assertEquals(0.0, overview.attendanceRate());
        }
    }

    @Nested
    @DisplayName("Trends, Distribution, TopEvents & Dashboard Tests")
    class DetailedQueryTests {

        @Test
        @DisplayName("Should return monthly trends")
        void shouldReturnMonthlyTrends() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            List<MonthlyTrendRow> mockRows = List.of(
                    new MonthlyTrendRow("2026-08", 2, 50, 45),
                    new MonthlyTrendRow("2026-09", 3, 80, 70)
            );
            when(fanpageStatisticRepositoryImpl.getMonthlyTrends(eq(fanpageId), anyInt(), any())).thenReturn(mockRows);

            List<FanpageMonthlyTrendDto> result = service.getMonthlyTrends(fanpageId, 2026, 5L);

            assertEquals(2, result.size());
            assertEquals("2026-08", result.get(0).monthYear());
            assertEquals(50, result.get(0).registrationCount());
        }

        @Test
        @DisplayName("Should return event type distribution with percentages")
        void shouldReturnEventDistribution() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            when(fanpageStatisticRepository.countEventsByFanpageId(fanpageId)).thenReturn(4L);
            List<EventTypeDistributionRow> mockRows = List.of(
                    new EventTypeDistributionRow("Workshop", 3),
                    new EventTypeDistributionRow("Seminar", 1)
            );
            when(fanpageStatisticRepositoryImpl.getEventDistribution(fanpageId)).thenReturn(mockRows);

            List<FanpageEventDistributionDto> result = service.getEventDistribution(fanpageId);

            assertEquals(2, result.size());
            assertEquals("Workshop", result.get(0).eventTypeName());
            assertEquals(75.0, result.get(0).percentage());
            assertEquals(25.0, result.get(1).percentage());
        }

        @Test
        @DisplayName("Should return top events sorted with attendance rate")
        void shouldReturnTopEvents() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            Instant now = Instant.now();
            List<TopEventRow> mockRows = List.of(
                    new TopEventRow(10L, "AI Day", "url1", now, 100, 100, 95)
            );
            when(fanpageStatisticRepositoryImpl.getTopEvents(fanpageId, 5)).thenReturn(mockRows);

            List<FanpageTopEventDto> result = service.getTopEvents(fanpageId, 5);

            assertEquals(1, result.size());
            assertEquals(10L, result.get(0).eventId());
            assertEquals(95.0, result.get(0).attendanceRate());
        }

        @Test
        @DisplayName("Should return composite dashboard")
        void shouldReturnDashboard() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            when(fanpageStatisticRepositoryImpl.getOverviewRow(fanpageId))
                    .thenReturn(new FanpageOverviewRow(2, 10, 2, 50, 40));
            when(followingRepository.countByFanpageIdAndFolStatus(fanpageId, FollowingStatusEnum.FOLLOWING))
                    .thenReturn(10L);
            when(fanpageStatisticRepositoryImpl.getMonthlyTrends(eq(fanpageId), anyInt(), any()))
                    .thenReturn(List.of());
            when(fanpageStatisticRepositoryImpl.getEventDistribution(fanpageId))
                    .thenReturn(List.of());
            when(fanpageStatisticRepositoryImpl.getTopEvents(fanpageId, 5))
                    .thenReturn(List.of());

            FanpageDashboardDto dashboard = service.getDashboard(fanpageId, 2026);

            assertNotNull(dashboard);
            assertEquals(2, dashboard.overview().totalEvents());
            assertEquals(80.0, dashboard.overview().attendanceRate());
        }

        @Test
        @DisplayName("Should return monthly absent rates")
        void shouldReturnMonthlyAbsentRates() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            List<MonthlyAbsentRateRow> mockRows = List.of(
                    new MonthlyAbsentRateRow(9, "Sep", 50, 10, 20.0),
                    new MonthlyAbsentRateRow(10, "Oct", 40, 4, 10.0)
            );
            when(fanpageStatisticRepositoryImpl.getMonthlyAbsentRate(fanpageId, 2026, 5L))
                    .thenReturn(mockRows);

            List<FanpageMonthlyAbsentRateDto> result = service.getMonthlyAbsentRate(fanpageId, 2026, 5L);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(9, result.get(0).month());
            assertEquals("Sep", result.get(0).monthName());
            assertEquals(20.0, result.get(0).absentRate());
            assertEquals(10, result.get(1).month());
            assertEquals(10.0, result.get(1).absentRate());
        }

        @Test
        @DisplayName("Should return student absence counts")
        void shouldReturnStudentAbsenceCounts() {
            when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(new Fanpage()));
            when(securityContextPort.getCurrentUserId()).thenReturn(userId);
            when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

            List<StudentAbsenceRow> mockRows = List.of(
                    new StudentAbsenceRow("SE170123", 42L, "Nguyen Van A", "a@fpt.edu.vn", "avatar.png", 4)
            );
            when(fanpageStatisticRepositoryImpl.getStudentAbsenceCounts(fanpageId, 5L, 3))
                    .thenReturn(mockRows);

            List<FanpageStudentAbsenceDto> result = service.getStudentAbsenceCounts(fanpageId, 5L, 3);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("SE170123", result.get(0).studentId());
            assertEquals(42L, result.get(0).userId());
            assertEquals(4, result.get(0).absenceCount());
        }
    }
}
