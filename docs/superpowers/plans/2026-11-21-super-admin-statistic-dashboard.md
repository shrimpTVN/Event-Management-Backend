# Super Admin Statistic Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a comprehensive university-wide analytics dashboard for Super Administrators (`AdminStatisticController`), supporting composite dashboard retrieval, modular analytical endpoints, and filtering by semester and year.

**Architecture:** 
Following [`effective_statistic_implementation_with_jdbcclient.md`](file:///home/shrimp/code/ddd/docs/artifact/effective_statistic_implementation_with_jdbcclient.md):
Aggregated read models use Spring 6 `JdbcClient` querying external `.sql` files in `ddd-infrastructure/src/main/resources/sql/statistic/admin/`. Data flows through a domain repository port, an application query service (`AdminStatisticQueryService`), and a dedicated REST controller (`AdminStatisticController`) returning `BaseResponse<T>`.

**Tech Stack:** Java 21 records, Spring Boot 3 (`JdbcClient`), MapStruct, JUnit 5, Mockito.

**Spec:** [`docs/superpowers/specs/2026-11-21-super-admin-statistic-dashboard-design.md`](file:///home/shrimp/code/ddd/docs/superpowers/specs/2026-11-21-super-admin-statistic-dashboard-design.md)

---

### Task 1: External SQL Queries & Infrastructure Projection Rows

**Files:**
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_users.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_fanpages.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_events.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_overview_attendance.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_monthly_trends.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_event_distribution.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_top_fanpages.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_cohort.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_school.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_demographics_major.sql`
- Create: `ddd-infrastructure/src/main/resources/sql/statistic/admin/admin_monthly_absent_rate.sql`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/dto/row/admin/*.java`

- [x] **Step 1: Write external SQL query files in `ddd-infrastructure`**
  - Parameterize all event-level queries with `(:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)` and `(:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)`.
- [x] **Step 2: Create JDBC projection records in `com.ddd.infrastructure.dto.row.admin`**
  - `AdminUserOverviewRow`, `AdminFanpageOverviewRow`, `AdminEventOverviewRow`, `AdminAttendanceOverviewRow`, `AdminMonthlyTrendRow`, `AdminEventTypeDistributionRow`, `AdminTopFanpageRow`.
- [x] **Step 3: Commit Task 1**

---

### Task 2: Domain Port & Infrastructure Repository Implementation

**Files:**
- Create: `ddd-domain/src/main/java/com/ddd/domain/repository/AdminStatisticRepository.java`
- Create: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/AdminStatisticRepositoryImpl.java`

- [x] **Step 1: Define `AdminStatisticRepository` in `ddd-domain`**
- [x] **Step 2: Implement `AdminStatisticRepositoryImpl` in `ddd-infrastructure` using `JdbcClient` and `SqlLoader`**
- [x] **Step 3: Verify module compilation with `./mvnw compile -pl ddd-domain,ddd-infrastructure`**
- [x] **Step 4: Commit Task 2**

---

### Task 3: Application DTOs, Mapper & Query Service

**Files:**
- Create: `ddd-application/src/main/java/com/ddd/application/dto/statistic/admin/*.java`
- Create: `ddd-application/src/main/java/com/ddd/application/mapper/AdminStatisticDtoMapper.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/admin/AdminStatisticQueryService.java`
- Create: `ddd-application/src/main/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/admin/impl/AdminStatisticQueryServiceImplTest.java`

- [x] **Step 1: Create application-layer DTO records**
  - `AdminDashboardDto`, `AdminOverviewStatisticDto`, `AdminUserOverviewDto`, `AdminFanpageOverviewDto`, `AdminEventOverviewDto`, `AdminAttendanceOverviewDto`, `AdminMonthlyTrendDto`, `AdminEventTypeDistributionDto`, `AdminTopFanpageDto`, `AdminDemographicsDto`, `AdminMonthlyAbsentRateDto`.
- [x] **Step 2: Create MapStruct `AdminStatisticDtoMapper`**
- [x] **Step 3: Implement `AdminStatisticQueryServiceImpl` with `@Transactional(readOnly = true)` and role check `securityContextPort.hasRole("ADMIN")`**
- [x] **Step 4: Write unit tests in `AdminStatisticQueryServiceImplTest`**
- [x] **Step 5: Run tests via `./mvnw test -pl ddd-application -Dtest=AdminStatisticQueryServiceImplTest`**
- [x] **Step 6: Commit Task 3**

---

### Task 4: API DTOs, Mapper & REST Controller

**Files:**
- Create: `ddd-api/src/main/java/com/ddd/api/dto/statistic/res/admin/*.java`
- Create: `ddd-api/src/main/java/com/ddd/api/mapper/AdminStatisticApiMapper.java`
- Create: `ddd-api/src/main/java/com/ddd/api/controller/AdminStatisticController.java`
- Test: `ddd-api/src/test/java/com/ddd/api/controller/AdminStatisticControllerTest.java`

- [x] **Step 1: Create API response DTOs under `com.ddd.api.dto.statistic.res.admin`**
- [x] **Step 2: Create `AdminStatisticApiMapper`**
- [x] **Step 3: Implement `AdminStatisticController` under `@RequestMapping("/admin/statistics")`**
  - `GET /dashboard`, `GET /overview`, `GET /trends`, `GET /event-distribution`, `GET /top-fanpages`, `GET /demographics`, `GET /absent-rate`
- [x] **Step 4: Write controller unit tests in `AdminStatisticControllerTest`**
- [x] **Step 5: Run tests via `./mvnw test -pl ddd-api -Dtest=AdminStatisticControllerTest`**
- [x] **Step 6: Commit Task 4**

---

### Task 5: Full Verification & Integration Check

- [x] **Step 1: Run full Maven compile across all 6 modules (`./mvnw clean compile`)**
- [x] **Step 2: Run all statistic test suites (`./mvnw test -Dtest=*Statistic* -Dsurefire.failIfNoSpecifiedTests=false`)**
- [x] **Step 3: Verify clean git status and commit history**
