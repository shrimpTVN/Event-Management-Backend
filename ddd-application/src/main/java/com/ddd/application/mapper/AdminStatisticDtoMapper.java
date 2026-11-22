package com.ddd.application.mapper;

import com.ddd.application.dto.statistic.admin.*;
import com.ddd.infrastructure.dto.row.admin.*;
import com.ddd.infrastructure.dto.row.fanpage.DemographicItemRow;
import com.ddd.infrastructure.dto.row.fanpage.MonthlyAbsentRateRow;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminStatisticDtoMapper {

    AdminUserOverviewDto toUserOverviewDto(AdminUserOverviewRow row);

    AdminFanpageOverviewDto toFanpageOverviewDto(AdminFanpageOverviewRow row);

    AdminEventOverviewDto toEventOverviewDto(AdminEventOverviewRow row);

    AdminAttendanceOverviewDto toAttendanceOverviewDto(AdminAttendanceOverviewRow row);

    default AdminOverviewStatisticDto toOverviewDto(
            AdminUserOverviewRow userRow,
            AdminFanpageOverviewRow fanpageRow,
            AdminEventOverviewRow eventRow,
            AdminAttendanceOverviewRow attendanceRow
    ) {
        return new AdminOverviewStatisticDto(
                toUserOverviewDto(userRow),
                toFanpageOverviewDto(fanpageRow),
                toEventOverviewDto(eventRow),
                toAttendanceOverviewDto(attendanceRow)
        );
    }

    AdminMonthlyTrendDto toMonthlyTrendDto(AdminMonthlyTrendRow row);

    List<AdminMonthlyTrendDto> toMonthlyTrendDtoList(List<AdminMonthlyTrendRow> rows);

    default AdminEventTypeDistributionDto toDistributionDto(AdminEventTypeDistributionRow row, long totalEvents) {
        if (row == null) return null;
        double percentage = totalEvents == 0 ? 0.0
                : Math.round((double) row.eventCount() / totalEvents * 10000.0) / 100.0;
        return new AdminEventTypeDistributionDto(row.eventTypeId(), row.eventTypeName(), row.eventCount(), percentage);
    }

    default List<AdminEventTypeDistributionDto> toDistributionDtoList(List<AdminEventTypeDistributionRow> rows, long totalEvents) {
        if (rows == null) return List.of();
        return rows.stream().map(r -> toDistributionDto(r, totalEvents)).toList();
    }

    AdminTopFanpageDto toTopFanpageDto(AdminTopFanpageRow row);

    List<AdminTopFanpageDto> toTopFanpageDtoList(List<AdminTopFanpageRow> rows);

    default AdminDemographicItemDto toDemographicItemDto(DemographicItemRow row, long totalRegistrations) {
        if (row == null) return null;
        double percentage = totalRegistrations == 0 ? 0.0
                : Math.round((double) row.count() / totalRegistrations * 10000.0) / 100.0;
        return new AdminDemographicItemDto(row.label(), row.count(), percentage);
    }

    default List<AdminDemographicItemDto> toDemographicDtoList(List<DemographicItemRow> rows, long totalRegistrations) {
        if (rows == null) return List.of();
        return rows.stream().map(r -> toDemographicItemDto(r, totalRegistrations)).toList();
    }

    default AdminDemographicsDto toDemographicsDto(
            List<DemographicItemRow> cohortRows,
            List<DemographicItemRow> schoolRows,
            List<DemographicItemRow> majorRows
    ) {
        long totalRegistrations = (cohortRows != null)
                ? cohortRows.stream().mapToLong(DemographicItemRow::count).sum()
                : 0L;
        List<AdminDemographicItemDto> cohorts = toDemographicDtoList(cohortRows, totalRegistrations);
        List<AdminDemographicItemDto> schools = toDemographicDtoList(schoolRows, totalRegistrations);
        List<AdminDemographicItemDto> majors = toDemographicDtoList(majorRows, totalRegistrations);
        return new AdminDemographicsDto(totalRegistrations, cohorts, schools, majors);
    }

    AdminMonthlyAbsentRateDto toMonthlyAbsentRateDto(MonthlyAbsentRateRow row);

    List<AdminMonthlyAbsentRateDto> toMonthlyAbsentRateDtoList(List<MonthlyAbsentRateRow> rows);

    default AdminDashboardDto toDashboardDto(
            AdminOverviewStatisticDto overview,
            List<AdminMonthlyTrendDto> monthlyTrends,
            List<AdminEventTypeDistributionDto> eventDistribution,
            List<AdminTopFanpageDto> topFanpages,
            AdminDemographicsDto demographics,
            List<AdminMonthlyAbsentRateDto> monthlyAbsentRates
    ) {
        return new AdminDashboardDto(
                overview,
                monthlyTrends,
                eventDistribution,
                topFanpages,
                demographics,
                monthlyAbsentRates
        );
    }
}
