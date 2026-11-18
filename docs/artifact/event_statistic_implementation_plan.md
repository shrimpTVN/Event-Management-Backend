# Event Statistics Dashboard — Architecture & Implementation Plan

> **Document Version**: 1.0.0  
> **Target Audience**: Backend Engineers, Tech Leads  
> **Status**: Awaiting User Approval  
> **Storage Path**: `docs/artifact/event_statistic_implementation_plan.md`  

---

## 1. Overview & Business Objectives

When a fanpage member (organizer, club leader, or volunteer) views an event's detail page, they need real-time operational metrics to monitor:
1. **Capacity & Registration Fill Rate**: How fast the event is booking up relative to total and gender quotas.
2. **Attendance Status Funnel**: Real-time count of students across every lifecycle state (`REGISTERED`, `SELF_CHECKED_IN`, `CHECKED_IN`, `SPENDING_CONFIRM`, `CONFIRMED`, `COMPLETED`, `ABSENT`, `CANCELLED`).
3. **Student Demographics**: Insights on student audience distribution by academic cohort (`kNumber`), major/faculty, and gender.
4. **Live Check-in Timeline**: Check-in velocity over time (15-minute intervals) to detect peak arrival times and venue congestion.

---

## 2. API Design & Routing

All endpoints are nested under the fanpage and event hierarchy to enforce tenant isolation:

```http
GET /fanpages/{fanpageId}/events/{eventId}/statistics
```

### Endpoints Specification

| Method | Sub-path | Description | Response Envelope |
|---|---|---|---|
| `GET` | `/fanpages/{fanpageId}/events/{eventId}/statistics/overview` | Summary KPIs (capacity, registered, attended, attendance rate) | `BaseResponse<EventOverviewStatisticResponseDto>` |
| `GET` | `/fanpages/{fanpageId}/events/{eventId}/statistics/status-funnel` | Breakdown of registrations across all 8 statuses | `BaseResponse<EventStatusFunnelResponseDto>` |
| `GET` | `/fanpages/{fanpageId}/events/{eventId}/statistics/demographics` | Cohort, major, and gender distributions | `BaseResponse<EventDemographicsResponseDto>` |
| `GET` | `/fanpages/{fanpageId}/events/{eventId}/statistics/timeline` | 15-minute interval check-in progression | `BaseResponse<List<EventCheckInTimelineResponseDto>>` |
| `GET` | `/fanpages/{fanpageId}/events/{eventId}/statistics` | **Composite Dashboard** (combines all 4 sections in 1 roundtrip) | `BaseResponse<EventDashboardStatisticResponseDto>` |

---

## 3. Authorization & Security Contract

* **Target Roles**: Any active member of the fanpage (`fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)`) or system administrator (`securityContextPort.hasRole("ADMIN")`).
* **Cross-Tenant Guard (Anti-IDOR)**: Service must verify `eventJpaRepository.existsByIdAndFanpageId(eventId, fanpageId)` before querying data. If mismatched, throws `ResourceNotFoundException`.
* **Current User Resolution**: Extracted purely via `SecurityContextPort` inside the application service.

```java
private void assertCallerCanViewEventStatistics(Long fanpageId, Long eventId) {
    Long currentUserId = securityContextPort.getCurrentUserId();
    if (currentUserId == null) {
        throw new UnauthorizedException("User is not authenticated");
    }
    if (!eventJpaRepository.existsByIdAndFanpageId(eventId, fanpageId)) {
        throw new ResourceNotFoundException("Event not found with id " + eventId + " in fanpage " + fanpageId);
    }
    if (securityContextPort.hasRole("ADMIN")) {
        return;
    }
    if (!fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)) {
        throw new AuthException("You do not have permission to view statistics for this event");
    }
}
```

---

## 4. Query & Database Aggregation Strategy

Following the proven **`JdbcClient` + External `.sql` files** architecture in `ddd-infrastructure`:

### 4.1 SQL Files (`ddd-infrastructure/src/main/resources/sql/statistic/`)

1. **`event_overview.sql`**:
   - Queries `events e` left joined with `registrations r`.
   - Computes:
     - `capacity`, `male_quantity`, `female_quantity`
     - `total_registrations`, `remaining_capacity`
     - `capacity_utilization_rate` (`total_registrations / capacity * 100%`)
     - `total_attended` (`FILTER (WHERE r.status IN ('CHECKED_IN', 'SELF_CHECKED_IN', 'SPENDING_CONFIRM', 'CONFIRMED', 'COMPLETED'))`)
     - `attendance_rate` (`total_attended / total_registrations * 100%`)

2. **`event_status_funnel.sql`**:
   - Single-pass conditional aggregation (`COUNT(r.user_id) FILTER (WHERE r.status = '...')`) for:
     - `REGISTERED`, `SELF_CHECKED_IN`, `CHECKED_IN`, `SPENDING_CONFIRM`, `CONFIRMED`, `COMPLETED`, `ABSENT`, `CANCELLED`.

3. **`event_demographics.sql`** (or granular demographic queries):
   - **Cohorts**: Grouped by `sp.k_number` with percentages.
   - **Majors**: Grouped by `m.name` with percentages.
   - **Gender**: Grouped by `u.gender` with male/female count & percentage.

4. **`event_checkin_timeline.sql`**:
   - PostgreSQL 15-minute interval binning:
     ```sql
     SELECT 
         TO_CHAR(date_bin('15 minutes', r.check_in_at, TIMESTAMP '2000-01-01 00:00:00'), 'YYYY-MM-DD HH24:MI') AS time_slot,
         COUNT(r.user_id) AS check_in_count
     FROM registrations r
     WHERE r.event_id = :eventId
       AND r.check_in_at IS NOT NULL
     GROUP BY time_slot
     ORDER BY time_slot ASC;
     ```

---

## 5. Architecture & File Structure

```
ddd-domain/
└── src/main/java/com/ddd/domain/
    └── repository/
        └── EventStatisticRepository.java             ← Domain port interface

ddd-infrastructure/
├── src/main/resources/sql/statistic/
│   ├── event_overview.sql                            ← External SQL
│   ├── event_status_funnel.sql
│   ├── event_demographics_cohort.sql
│   ├── event_demographics_major.sql
│   ├── event_demographics_gender.sql
│   └── event_checkin_timeline.sql
└── src/main/java/com/ddd/infrastructure/repository/
    ├── EventStatisticRepositoryImpl.java             ← JdbcClient implementation
    └── row/event/
        ├── EventOverviewRow.java                     ← Internal JDBC mapping records
        ├── EventStatusFunnelRow.java
        ├── DemographicItemRow.java
        └── CheckInTimelineRow.java

ddd-application/
└── src/main/java/com/ddd/application/
    ├── dto/statistic/event/
    │   ├── EventOverviewStatisticDto.java            ← Application DTOs
    │   ├── EventStatusFunnelDto.java
    │   ├── EventDemographicsDto.java
    │   ├── EventCheckInTimelineDto.java
    │   └── EventDashboardStatisticDto.java
    └── service/event/
        ├── EventStatisticQueryService.java           ← Service interface
        └── impl/
            └── EventStatisticQueryServiceImpl.java   ← Implementation & guards

ddd-api/
└── src/main/java/com/ddd/api/
    ├── dto/statistic/event/res/
    │   ├── EventOverviewStatisticResponseDto.java    ← API Response records
    │   ├── EventStatusFunnelResponseDto.java
    │   ├── EventDemographicsResponseDto.java
    │   ├── EventCheckInTimelineResponseDto.java
    │   └── EventDashboardStatisticResponseDto.java
    ├── mapper/
    │   └── EventStatisticApiMapper.java              ← MapStruct mapper
    └── controller/
        └── EventStatisticController.java             ← REST controller
```

---

## 6. Implementation Steps

1. **Step 1: Domain Layer (`ddd-domain`)**
   - Create `EventStatisticRepository` interface.
2. **Step 2: Infrastructure Layer (`ddd-infrastructure`)**
   - Add SQL queries under `sql/statistic/`.
   - Create row records under `com.ddd.infrastructure.repository.row.event`.
   - Implement `EventStatisticRepositoryImpl` using `JdbcClient` and `SqlLoader`.
3. **Step 3: Application Layer (`ddd-application`)**
   - Create DTOs under `com.ddd.application.dto.statistic.event`.
   - Implement `EventStatisticQueryService` & `EventStatisticQueryServiceImpl` with member check and cross-tenant guard.
4. **Step 4: API Layer (`ddd-api`)**
   - Create response records under `com.ddd.api.dto.statistic.event.res`.
   - Create MapStruct `EventStatisticApiMapper`.
   - Implement `EventStatisticController` returning `BaseResponse<T>`.
5. **Step 5: Testing & Verification**
   - Write unit tests for `EventStatisticQueryServiceImplTest` (testing permission checks, cross-tenant isolation, division-by-zero guards).
   - Write controller tests for `EventStatisticControllerTest`.
   - Verify full Maven compilation with `./mvnw compile`.
