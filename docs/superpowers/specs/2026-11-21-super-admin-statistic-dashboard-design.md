# Super Admin Statistic Dashboard Design Specification

## 1. Overview & Business Intent
Super Administrators require a comprehensive university-wide analytics dashboard in the administrative portal (`CMS`) to monitor user demographics, club/fanpage activity, event performance, attendance reliability, and absenteeism rates across academic semesters and calendar years.

## 2. Key Architecture & Security
- **RBAC Security Guard**: All administrative statistics endpoints reside under `/admin/statistics/**` and require role `ADMIN` verified by `SecurityContextPort.hasRole("ADMIN")` or Spring Security `@PreAuthorize("hasRole('ADMIN')")`.
- **Query Strategy**: Uses Spring 6 `JdbcClient` with external `.sql` files loaded via `SqlResourceLoader` under `ddd-infrastructure/src/main/resources/sql/statistic/admin/` to eliminate Hibernate entity loading overhead and memory bloat on large aggregations.
- **Filtering Capability**: Supports optional `@RequestParam(required = false) Integer year` and `@RequestParam(required = false) Long semesterId` across analytics queries.

## 3. Metrics Breakdown & Endpoints

### 3.1 Composite Dashboard
* **Endpoint**: `GET /admin/statistics/dashboard`
* **Query Params**: `year` (optional), `semesterId` (optional)
* **Response Model**: `AdminDashboardResponseDto`
  - `overview`: `AdminOverviewStatisticResponseDto`
    - `users`: `totalUsers`, `totalStudents`, `totalStaff`, `totalActiveUsers`
    - `fanpages`: `totalFanpages`, `activeFanpages`, `pendingApprovalFanpages`, `bannedFanpages`
    - `events`: `totalEvents`, `publishedEvents`, `completedEvents`, `cancelledEvents`
    - `attendance`: `totalRegistrations`, `totalAttendees`, `absentCount`, `attendanceRate`, `absentRate`
  - `monthlyTrends`: List of `AdminMonthlyTrendResponseDto` (`monthYear`, `eventCount`, `registrationCount`, `attendeeCount`, `absentCount`, `absentRate`)
  - `eventsByType`: List of `AdminEventTypeDistributionResponseDto` (`eventTypeId`, `eventTypeName`, `eventCount`, `percentage`)
  - `topFanpages`: List of `AdminTopFanpageResponseDto` (`fanpageId`, `fanpageName`, `avatarUrl`, `eventCount`, `followerCount`, `totalAttendees`)
  - `demographics`: `AdminDemographicsResponseDto` (`cohorts`, `schools`, `majors`)
  - `monthlyAbsentRates`: List of `AdminMonthlyAbsentRateResponseDto` (`month`, `monthName`, `totalRegistrations`, `absentCount`, `absentRate`)

### 3.2 Granular Modular Endpoints
1. `GET /admin/statistics/overview?year=...&semesterId=...`
2. `GET /admin/statistics/trends?year=...&semesterId=...`
3. `GET /admin/statistics/event-distribution?year=...&semesterId=...`
4. `GET /admin/statistics/top-fanpages?year=...&semesterId=...&limit=5`
5. `GET /admin/statistics/demographics?year=...&semesterId=...`
6. `GET /admin/statistics/absent-rate?year=...&semesterId=...`

## 4. Multi-Module DDD Decomposition
* **`ddd-domain`**:
  - `AdminStatisticRepository` port interface.
* **`ddd-infrastructure`**:
  - External SQL files in `sql/statistic/admin/*.sql`
  - Projection rows: `AdminUserOverviewRow`, `AdminFanpageOverviewRow`, `AdminEventOverviewRow`, `AdminAttendanceOverviewRow`, `AdminMonthlyTrendRow`, `AdminEventTypeDistributionRow`, `AdminTopFanpageRow`.
  - `AdminStatisticRepositoryImpl` implementing `AdminStatisticRepository` with `JdbcClient`.
* **`ddd-application`**:
  - DTOs: `AdminDashboardDto`, `AdminOverviewStatisticDto`, `AdminMonthlyTrendDto`, `AdminEventTypeDistributionDto`, `AdminTopFanpageDto`, `AdminDemographicsDto`, etc.
  - `AdminStatisticDtoMapper` MapStruct mapper.
  - `AdminStatisticQueryService` & `AdminStatisticQueryServiceImpl`.
* **`ddd-api`**:
  - Response DTOs in `com.ddd.api.dto.statistic.res.admin.*`.
  - `AdminStatisticApiMapper`.
  - `AdminStatisticController` under `@RequestMapping("/admin/statistics")`.
