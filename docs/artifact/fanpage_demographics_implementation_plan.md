# Fanpage Attendee Demographics (Cohort, School, Major) — Architecture & Implementation Plan

> **Document Version**: 1.0.0  
> **Target Audience**: Backend Engineers, Tech Leads  
> **Status**: Awaiting User Approval  
> **Storage Path**: `docs/artifact/fanpage_demographics_implementation_plan.md`  

---

## 1. Executive Summary & Requirements

This plan details the addition of **Student Attendee Demographics** to the **Fanpage Statistics Dashboard**.

### Target Metrics:
1. **Cohort Breakdown (`kNumber`)**: Distribution of registered students across academic batches (e.g., `K19`: 45%, `K18`: 35%, `K17`: 20%).
2. **School / Faculty Breakdown**: Distribution of registered students across schools (e.g., `School of Technology`: 65%, `School of Business`: 35%).
3. **Major Breakdown**: Distribution of registered students across majors (e.g., `Software Engineering`: 40%, `Information Assurance`: 25%).
4. **Percentage Calculation**: Calculated against **all active registrations** (`status != 'CANCELLED'`).
5. **Filters**: Support filtering by **`year`** (e.g., `2026`) and/or **`semesterId`**.

---

## 2. API Design

### Endpoints

| Method | Endpoint | Query Parameters | Description | Response Envelope |
|---|---|---|---|---|
| `GET` | `/fanpages/{fanpageId}/statistics/demographics` | `year` *(optional)*, `semesterId` *(optional)* | Demographic breakdown across cohorts, schools, and majors | `BaseResponse<FanpageDemographicsResponseDto>` |
| `GET` | `/fanpages/{fanpageId}/statistics/dashboard` | `year` *(optional)*, `semesterId` *(optional)* | Composite dashboard (enriched with `demographics`) | `BaseResponse<FanpageDashboardResponseDto>` |

### Sample Response: `GET /fanpages/{fanpageId}/statistics/demographics`
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "totalRegistrations": 450,
    "cohorts": [
      { "name": "K19", "count": 225, "percentage": 50.0 },
      { "name": "K18", "count": 135, "percentage": 30.0 },
      { "name": "K17", "count": 90,  "percentage": 20.0 }
    ],
    "schools": [
      { "name": "School of Technology", "count": 315, "percentage": 70.0 },
      { "name": "School of Business",   "count": 135, "percentage": 30.0 }
    ],
    "majors": [
      { "name": "Software Engineering",   "count": 225, "percentage": 50.0 },
      { "name": "International Business", "count": 135, "percentage": 30.0 },
      { "name": "Information Assurance",  "count": 90,  "percentage": 20.0 }
    ]
  }
}
```

---

## 3. Database Aggregation Strategy (`JdbcClient` + External SQL)

Create 3 dedicated SQL query files under `ddd-infrastructure/src/main/resources/sql/statistic/`:

1. **`fanpage_demographics_cohort.sql`**:
   - Joins `events e` $\rightarrow$ `registrations r` $\rightarrow$ `users u` $\rightarrow$ `student_profiles sp`.
   - Filters: `e.fanpage_id = :fanpageId AND e.is_active = true AND r.status != 'CANCELLED'`.
   - Optional filters: `(:year::int IS NULL OR EXTRACT(YEAR FROM e.date_happen) = :year::int)` and `(:semesterId::bigint IS NULL OR e.semester_id = :semesterId::bigint)`.
   - Groups by `sp.k_number` and formats as `'K' || sp.k_number` (or `'Unknown'`).

2. **`fanpage_demographics_school.sql`**:
   - Joins `events e` $\rightarrow$ `registrations r` $\rightarrow$ `users u` $\rightarrow$ `student_profiles sp` $\rightarrow$ `majors m` $\rightarrow$ `schools s`.
   - Groups by `s.name` (fallback to `'Other'`).

3. **`fanpage_demographics_major.sql`**:
   - Joins `events e` $\rightarrow$ `registrations r` $\rightarrow$ `users u` $\rightarrow$ `student_profiles sp` $\rightarrow$ `majors m`.
   - Groups by `m.name` (fallback to `'Other'`).

---

## 4. Architecture & File Structure

```
ddd-infrastructure/
├── src/main/resources/sql/statistic/
│   ├── fanpage_demographics_cohort.sql               ← NEW SQL
│   ├── fanpage_demographics_school.sql               ← NEW SQL
│   └── fanpage_demographics_major.sql                ← NEW SQL
└── src/main/java/com/ddd/infrastructure/repository/
    ├── row/
    │   └── DemographicItemRow.java                   ← NEW record (name, count)
    └── FanpageStatisticRepositoryImpl.java           ← Add demographic query methods

ddd-application/
└── src/main/java/com/ddd/application/
    ├── dto/statistic/
    │   ├── DemographicItemDto.java                   ← NEW record (name, count, percentage)
    │   ├── FanpageDemographicsDto.java               ← NEW record (cohorts, schools, majors)
    │   └── FanpageDashboardDto.java                  ← UPDATE (add FanpageDemographicsDto)
    └── service/fanpage/
        ├── FanpageStatisticQueryService.java         ← Add getDemographics, update getDashboard
        └── impl/
            └── FanpageStatisticQueryServiceImpl.java ← Implement calculation and filters

ddd-api/
└── src/main/java/com/ddd/api/
    ├── dto/statistic/res/
    │   ├── DemographicItemResponseDto.java           ← NEW record
    │   ├── FanpageDemographicsResponseDto.java       ← NEW record
    │   └── FanpageDashboardResponseDto.java          ← UPDATE (add FanpageDemographicsResponseDto)
    ├── mapper/
    │   └── FanpageStatisticApiMapper.java            ← Add mapping for demographics
    └── controller/
        └── FanpageStatisticController.java           ← Add /demographics & update /dashboard
```

---

## 5. Step-by-Step Implementation Roadmap

### Step 1: Infrastructure Layer (`ddd-infrastructure`)
- Add `fanpage_demographics_cohort.sql`, `fanpage_demographics_school.sql`, `fanpage_demographics_major.sql`.
- Create `DemographicItemRow(String name, long count)`.
- Update `FanpageStatisticRepositoryImpl` to add:
  - `getCohortDemographics(Long fanpageId, Integer year, Long semesterId)`
  - `getSchoolDemographics(Long fanpageId, Integer year, Long semesterId)`
  - `getMajorDemographics(Long fanpageId, Integer year, Long semesterId)`

### Step 2: Application Layer (`ddd-application`)
- Create `DemographicItemDto` and `FanpageDemographicsDto`.
- Update `FanpageDashboardDto` with `demographics`.
- Update `FanpageStatisticQueryService` interface.
- Implement in `FanpageStatisticQueryServiceImpl`:
  - Calculate total registrations from demographic counts.
  - Calculate percentages with division-by-zero protection and 2-decimal rounding.
  - Wire demographics into `getDashboard`.

### Step 3: API Layer (`ddd-api`)
- Create `DemographicItemResponseDto` and `FanpageDemographicsResponseDto`.
- Update `FanpageDashboardResponseDto`.
- Update `FanpageStatisticApiMapper`.
- Update `FanpageStatisticController`:
  - Add `GET /demographics` endpoint with `@RequestParam(required = false) Integer year` and `@RequestParam(required = false) Long semesterId`.
  - Update `GET /dashboard` endpoint to accept `year` and `semesterId`.

### Step 4: Verification & Testing
- Update unit tests in `FanpageStatisticQueryServiceImplTest` and `FanpageStatisticControllerTest`.
- Run `./mvnw compile` to verify clean build and MapStruct generation.
- Execute unit tests verifying calculations, zero-registration handling, and filter parameters.
