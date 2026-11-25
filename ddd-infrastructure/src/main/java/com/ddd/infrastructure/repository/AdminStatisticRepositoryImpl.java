package com.ddd.infrastructure.repository;

import com.ddd.domain.repository.AdminStatisticRepository;
import com.ddd.infrastructure.dto.row.admin.*;
import com.ddd.infrastructure.dto.row.fanpage.DemographicItemRow;
import com.ddd.infrastructure.dto.row.fanpage.MonthlyAbsentRateRow;
import com.ddd.infrastructure.utils.SqlLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminStatisticRepositoryImpl implements AdminStatisticRepository {

    private static final String OVERVIEW_USERS_SQL = SqlLoader.load("statistic/admin/admin_overview_users.sql");
    private static final String OVERVIEW_FANPAGES_SQL = SqlLoader.load("statistic/admin/admin_overview_fanpages.sql");
    private static final String OVERVIEW_EVENTS_SQL = SqlLoader.load("statistic/admin/admin_overview_events.sql");
    private static final String OVERVIEW_ATTENDANCE_SQL = SqlLoader.load("statistic/admin/admin_overview_attendance.sql");
    private static final String MONTHLY_TRENDS_SQL = SqlLoader.load("statistic/admin/admin_monthly_trends.sql");
    private static final String EVENT_DIST_SQL = SqlLoader.load("statistic/admin/admin_event_distribution.sql");
    private static final String TOP_FANPAGES_SQL = SqlLoader.load("statistic/admin/admin_top_fanpages.sql");
    private static final String DEMO_COHORT_SQL = SqlLoader.load("statistic/admin/admin_demographics_cohort.sql");
    private static final String DEMO_SCHOOL_SQL = SqlLoader.load("statistic/admin/admin_demographics_school.sql");
    private static final String DEMO_MAJOR_SQL = SqlLoader.load("statistic/admin/admin_demographics_major.sql");
    private static final String MONTHLY_ABSENT_RATE_SQL = SqlLoader.load("statistic/admin/admin_monthly_absent_rate.sql");

    private final JdbcClient jdbcClient;

    @Override
    public long countTotalUsers() {
        return jdbcClient.sql("SELECT COUNT(id) FROM users WHERE is_active = true")
                .query(Long.class)
                .single();
    }

    @Override
    public long countTotalFanpages() {
        return jdbcClient.sql("SELECT COUNT(id) FROM fanpages WHERE is_active = true")
                .query(Long.class)
                .single();
    }

    @Override
    public long countTotalEvents() {
        return jdbcClient.sql("SELECT COUNT(id) FROM events WHERE is_active = true")
                .query(Long.class)
                .single();
    }

    public AdminUserOverviewRow getUserOverview() {
        return jdbcClient.sql(OVERVIEW_USERS_SQL)
                .query(AdminUserOverviewRow.class)
                .single();
    }

    public AdminFanpageOverviewRow getFanpageOverview() {
        return jdbcClient.sql(OVERVIEW_FANPAGES_SQL)
                .query(AdminFanpageOverviewRow.class)
                .single();
    }

    public AdminEventOverviewRow getEventOverview(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(OVERVIEW_EVENTS_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(AdminEventOverviewRow.class)
                .single();
    }

    public AdminAttendanceOverviewRow getAttendanceOverview(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(OVERVIEW_ATTENDANCE_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(AdminAttendanceOverviewRow.class)
                .single();
    }

    public List<AdminMonthlyTrendRow> getMonthlyTrends(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(MONTHLY_TRENDS_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(AdminMonthlyTrendRow.class)
                .list();
    }

    public List<AdminEventTypeDistributionRow> getEventDistribution(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(EVENT_DIST_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(AdminEventTypeDistributionRow.class)
                .list();
    }

    public List<AdminTopFanpageRow> getTopFanpages(Integer year, Long semesterId, Integer limit, String academicYear) {
        return jdbcClient.sql(TOP_FANPAGES_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("limit", limit)
                .param("academicYear", academicYear)
                .query(AdminTopFanpageRow.class)
                .list();
    }

    public List<DemographicItemRow> getCohortDemographics(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(DEMO_COHORT_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getSchoolDemographics(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(DEMO_SCHOOL_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<DemographicItemRow> getMajorDemographics(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(DEMO_MAJOR_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(DemographicItemRow.class)
                .list();
    }

    public List<MonthlyAbsentRateRow> getMonthlyAbsentRate(Integer year, Long semesterId, String academicYear) {
        return jdbcClient.sql(MONTHLY_ABSENT_RATE_SQL)
                .param("year", year)
                .param("semesterId", semesterId)
                .param("academicYear", academicYear)
                .query(MonthlyAbsentRateRow.class)
                .list();
    }
}
