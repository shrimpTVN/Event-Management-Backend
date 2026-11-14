package com.ddd.infrastructure.repository;

import com.ddd.infrastructure.dto.row.event.CheckInTimelineRow;
import com.ddd.infrastructure.dto.row.event.DemographicItemRow;
import com.ddd.infrastructure.dto.row.event.EventOverviewRow;
import com.ddd.infrastructure.dto.row.event.EventStatusFunnelRow;
import com.ddd.infrastructure.utils.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventStatisticRepository {
    private static final String OVERVIEW_SQL = SqlLoader.load("statistic/event/event_overview.sql");
    private static final String STATUS_FUNNEL_SQL = SqlLoader.load("statistic/event/event_status_funnel.sql");
    private static final String DEMO_COHORT_SQL = SqlLoader.load("statistic/event/event_demographics_cohort.sql");
    private static final String DEMO_MAJOR_SQL = SqlLoader.load("statistic/event/event_demographics_major.sql");
    private static final String DEMO_GENDER_SQL = SqlLoader.load("statistic/event/event_demographics_gender.sql");
    private static final String TIMELINE_SQL = SqlLoader.load("statistic/event/event_checkin_timeline.sql");
    private final JdbcClient jdbcClient;

    public Optional<EventOverviewRow> getOverviewRow(Long eventId) {
        return jdbcClient.sql(OVERVIEW_SQL)
                .param("eventId", eventId)
                .query(EventOverviewRow.class)
                .optional();
    }

    public EventStatusFunnelRow getStatusFunnelRow(Long eventId) {
        return jdbcClient.sql(STATUS_FUNNEL_SQL)
                .param("eventId", eventId)
                .query(EventStatusFunnelRow.class)
                .single();
    }

    public List<DemographicItemRow> getCohortDemographics(Long eventId) {
        return jdbcClient.sql(DEMO_COHORT_SQL)
                .param("eventId", eventId)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getMajorDemographics(Long eventId) {
        return jdbcClient.sql(DEMO_MAJOR_SQL)
                .param("eventId", eventId)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getGenderDemographics(Long eventId) {
        return jdbcClient.sql(DEMO_GENDER_SQL)
                .param("eventId", eventId)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<CheckInTimelineRow> getCheckInTimeline(Long eventId) {
        return jdbcClient.sql(TIMELINE_SQL)
                .param("eventId", eventId)
                .query(CheckInTimelineRow.class)
                .list();
    }
}
