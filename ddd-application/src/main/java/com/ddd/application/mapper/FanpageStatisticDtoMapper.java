package com.ddd.application.mapper;

import com.ddd.application.dto.statistic.*;
import com.ddd.infrastructure.dto.row.fanpage.*;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FanpageStatisticDtoMapper {

    default FanpageOverviewStatisticDto toOverviewDto(FanpageOverviewRow row, long totalFollowers) {
        if (row == null) return null;
        double attendanceRate = row.totalRegistrations() == 0 ? 0.0
                : Math.round((double) row.totalAttendees() / row.totalRegistrations() * 10000.0) / 100.0;
        return new FanpageOverviewStatisticDto(
                row.totalEvents(),
                totalFollowers,
                row.totalMembers(),
                row.totalRegistrations(),
                row.totalAttendees(),
                attendanceRate
        );
    }

    FanpageMonthlyTrendDto toMonthlyTrendDto(MonthlyTrendRow row);

    List<FanpageMonthlyTrendDto> toMonthlyTrendDtoList(List<MonthlyTrendRow> rows);

    default FanpageEventDistributionDto toDistributionDto(EventTypeDistributionRow row, long totalEvents) {
        if (row == null) return null;
        double percentage = totalEvents == 0 ? 0.0
                : Math.round((double) row.eventCount() / totalEvents * 10000.0) / 100.0;
        return new FanpageEventDistributionDto(row.eventTypeName(), row.eventCount(), percentage);
    }

    default List<FanpageEventDistributionDto> toDistributionDtoList(List<EventTypeDistributionRow> rows, long totalEvents) {
        if (rows == null) return List.of();
        return rows.stream().map(r -> toDistributionDto(r, totalEvents)).toList();
    }

    default FanpageTopEventDto toTopEventDto(TopEventRow row) {
        if (row == null) return null;
        double attendanceRate = row.registrationCount() == 0 ? 0.0
                : Math.round((double) row.attendeeCount() / row.registrationCount() * 10000.0) / 100.0;
        return new FanpageTopEventDto(
                row.eventId(),
                row.eventName(),
                row.bannerUrl(),
                row.dateHappen(),
                row.capacity(),
                row.registrationCount(),
                row.attendeeCount(),
                attendanceRate
        );
    }

    default List<FanpageTopEventDto> toTopEventDtoList(List<TopEventRow> rows) {
        if (rows == null) return List.of();
        return rows.stream().map(this::toTopEventDto).toList();
    }

    default FanpageDemographicItemDto toDemographicItemDto(DemographicItemRow row, long totalRegistrations) {
        if (row == null) return null;
        double percentage = totalRegistrations == 0 ? 0.0
                : Math.round((double) row.count() / totalRegistrations * 10000.0) / 100.0;
        return new FanpageDemographicItemDto(row.label(), row.count(), percentage);
    }

    default List<FanpageDemographicItemDto> toDemographicDtoList(List<DemographicItemRow> rows, long totalRegistrations) {
        if (rows == null) return List.of();
        return rows.stream().map(r -> toDemographicItemDto(r, totalRegistrations)).toList();
    }

    default FanpageDemographicsDto toDemographicsDto(
            long totalRegistrations,
            List<FanpageDemographicItemDto> cohorts,
            List<FanpageDemographicItemDto> schools,
            List<FanpageDemographicItemDto> majors) {
        return new FanpageDemographicsDto(totalRegistrations, cohorts, schools, majors);
    }

    default FanpageDemographicsDto toDemographicsDto(
            List<DemographicItemRow> cohortRows,
            List<DemographicItemRow> schoolRows,
            List<DemographicItemRow> majorRows) {
        long totalRegistrations = (cohortRows != null)
                ? cohortRows.stream().mapToLong(DemographicItemRow::count).sum()
                : 0L;
        List<FanpageDemographicItemDto> cohorts = toDemographicDtoList(cohortRows, totalRegistrations);
        List<FanpageDemographicItemDto> schools = toDemographicDtoList(schoolRows, totalRegistrations);
        List<FanpageDemographicItemDto> majors = toDemographicDtoList(majorRows, totalRegistrations);
        return new FanpageDemographicsDto(totalRegistrations, cohorts, schools, majors);
    }

    default FanpageDashboardDto toDashboardDto(
            FanpageOverviewStatisticDto overview,
            List<FanpageMonthlyTrendDto> monthlyTrends,
            List<FanpageEventDistributionDto> eventsByType,
            List<FanpageTopEventDto> topEvents,
            FanpageDemographicsDto demographics) {
        return new FanpageDashboardDto(overview, monthlyTrends, eventsByType, topEvents, demographics);
    }
}
