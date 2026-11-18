package com.ddd.infrastructure.repository;

import com.ddd.domain.repository.FanpageStatisticRepository;
import com.ddd.infrastructure.dto.row.fanpage.*;
import com.ddd.infrastructure.utils.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FanpageStatisticRepositoryImpl implements FanpageStatisticRepository {

    private static final String OVERVIEW_SQL = SqlLoader.load("statistic/fanpage/fanpage_overview.sql");
    private static final String MONTHLY_TRENDS_SQL = SqlLoader.load("statistic/fanpage/fanpage_monthly_trends.sql");
    private static final String EVENT_DIST_SQL = SqlLoader.load("statistic/fanpage/fanpage_event_distribution.sql");
    private static final String TOP_EVENTS_SQL = SqlLoader.load("statistic/fanpage/fanpage_top_events.sql");
    private static final String MEMBER_AMOUNT_SQL = SqlLoader.load("statistic/fanpage/fanpage_member_amount.sql");
    private static final String REGISTRATION_AMOUNT_SQL = SqlLoader.load("statistic/fanpage/fanpage_registration_amount.sql");
    private static final String ATTENDEE_AMOUNT_SQL = SqlLoader.load("statistic/fanpage/fanpage_attendee_amount.sql");
    private static final String DEMO_COHORT_SQL = SqlLoader.load("statistic/fanpage/fanpage_demographics_cohort.sql");
    private static final String DEMO_SCHOOL_SQL = SqlLoader.load("statistic/fanpage/fanpage_demographics_school.sql");
    private static final String DEMO_MAJOR_SQL = SqlLoader.load("statistic/fanpage/fanpage_demographics_major.sql");
    private final JdbcClient jdbcClient;

    @Override
    public long countEventsByFanpageId(Long fanpageId) {
        return jdbcClient.sql("SELECT COUNT(e.id) AS event_amount " +
                        "FROM fanpages fp LEFT JOIN events e ON fp.id = e.fanpage_id " +
                        "WHERE fp.id = :fanpageId AND fp.is_active = true AND e.is_active = true;")
                .param("fanpageId", fanpageId)
                .query(Long.class)
                .single();
    }

    @Override
    public long countTotalRegistrationsByFanpageId(Long fanpageId) {
        return jdbcClient.sql(REGISTRATION_AMOUNT_SQL)
                .param("fanpageId", fanpageId)
                .query(Long.class)
                .single();
    }

    @Override
    public long countTotalAttendeesByFanpageId(Long fanpageId) {
        return jdbcClient.sql(ATTENDEE_AMOUNT_SQL)
                .param("fanpageId", fanpageId)
                .query(Long.class)
                .single();
    }

    @Override
    public long countActiveMembersByFanpageId(Long fanpageId) {
        return jdbcClient.sql(MEMBER_AMOUNT_SQL)
                .param("fanpageId", fanpageId)
                .query(Long.class)
                .single();
    }

    public FanpageOverviewRow getOverviewRow(Long fanpageId) {
        return jdbcClient.sql(OVERVIEW_SQL)
                .param("fanpageId", fanpageId)
                .query(FanpageOverviewRow.class)
                .single();
    }

    public List<MonthlyTrendRow> getMonthlyTrends(Long fanpageId, int year) {
        return jdbcClient.sql(MONTHLY_TRENDS_SQL)
                .param("fanpageId", fanpageId)
                .param("year", year)
                .query(MonthlyTrendRow.class)
                .list();
    }

    public List<EventTypeDistributionRow> getEventDistribution(Long fanpageId) {
        return jdbcClient.sql(EVENT_DIST_SQL)
                .param("fanpageId", fanpageId)
                .query(EventTypeDistributionRow.class)
                .list();
    }

    public List<TopEventRow> getTopEvents(Long fanpageId, int limit) {
        return jdbcClient.sql(TOP_EVENTS_SQL)
                .param("fanpageId", fanpageId)
                .param("limit", limit)
                .query(TopEventRow.class)
                .list();
    }

    public List<DemographicItemRow> getCohortDemographics(Long fanpageId, Integer year, Long semesterId) {
        return jdbcClient.sql(DEMO_COHORT_SQL)
                .param("fanpageId", fanpageId)
                .param("year", year)
                .param("semesterId", semesterId)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getSchoolDemographics(Long fanpageId, Integer year, Long semesterId) {
        return jdbcClient.sql(DEMO_SCHOOL_SQL)
                .param("fanpageId", fanpageId)
                .param("year", year)
                .param("semesterId", semesterId)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getMajorDemographics(Long fanpageId, Integer year, Long semesterId) {
        return jdbcClient.sql(DEMO_MAJOR_SQL)
                .param("fanpageId", fanpageId)
                .param("year", year)
                .param("semesterId", semesterId)
                .query(DemographicItemRow.class)
                .list();
    }
}
