# Bug Fixes & Architecture Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resolve all defects and data integrity bugs across Event DTOs, Superadmin user and member APIs, SV5T criteria transaction rollback handling, and Registration composite-key JPA repository performance.

**Architecture:** Strictly adheres to Domain-Driven Design (DDD) multi-module layers (`ddd-domain`, `ddd-infrastructure`, `ddd-application`, `ddd-api`). Business invariants and entity relationships remain in domain/application layers, Spring Security identity is retrieved exclusively via `SecurityContextPort`, controllers return `BaseResponse<T>`, and repository queries avoid N+1 and proxy overhead.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA / Hibernate 6, MapStruct, JUnit 5, Mockito, AssertJ.

**Spec:**
- [docs/bug/BUG_REPORT_EVENT_TYPE_ID.md](file:///home/shrimp/code/ddd/docs/bug/BUG_REPORT_EVENT_TYPE_ID.md)
- [docs/bug/superadmin-api-todo.md](file:///home/shrimp/code/ddd/docs/bug/superadmin-api-todo.md)
- [docs/bug/HUONG_DAN_SUA_LOI_BE_SV5T.md](file:///home/shrimp/code/ddd/docs/bug/HUONG_DAN_SUA_LOI_BE_SV5T.md)
- [docs/bug/registration-repository-issues-and-refactor-report.md](file:///home/shrimp/code/ddd/docs/bug/registration-repository-issues-and-refactor-report.md)
- [architecture_conventions.md](file:///home/shrimp/code/ddd/architecture_conventions.md)

## Global Constraints

- Domain layer (`ddd-domain`) must remain pure Java with ZERO Spring/JPA dependencies.
- Controllers in `ddd-api` must NEVER accept identity tokens from HTTP request parameters and pass them to services; application services must inject `SecurityContextPort`.
- Application service queries must return `*InfoDto` models; API controllers map `*InfoDto` to `*ResponseDto` via `*ApiMapper` and wrap with `BaseResponse<T>`.
- MapStruct mappers updating entities must ignore audit columns (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`).
- All pagination must use `ServiceUtil.createPageable(page, size, sortBy, sortDir)`.

## Review Focus

1. `markUncompleted` called on non-existent `StudentCriteriaProgress` record must exit idempotently without throwing `IllegalArgumentException` or triggering transaction rollback.
2. `UserSummaryDto.userId` must map from `UserJpaEntity.id` and must never be `null` in `/users/admin` and `/users/search-users`.
3. `EventInfoDto` and `EventResponseDto` must retain non-null `eventTypeId` and `criteriaId` whenever the underlying `EventJpaEntity` associations are present.
4. `RegistrationJpaRepository` composite ID operations must accept `RegistrationId` instead of `Long` to prevent class-cast and entity lookup failures.
5. `GET /fanpages/{fanpageId}/members` requested by a user with role `ADMIN` must return member records even when the fanpage has `isActive = false`.

---

### Task 1: Fix `UserDtoMapper` User ID Mapping

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/mapper/UserDtoMapper.java:22-26`
- Test: `ddd-application/src/test/java/com/ddd/application/mapper/UserDtoMapperTest.java`

**Interfaces:**
- Consumes: `UserJpaEntity.getId() -> Long`
- Produces: `UserSummaryDto.userId() -> Long`

- [ ] **Step 1: Write the failing unit test**

Create `ddd-application/src/test/java/com/ddd/application/mapper/UserDtoMapperTest.java`:
```java
package com.ddd.application.mapper;

import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserDtoMapperTest {
    private final UserDtoMapper mapper = Mappers.getMapper(UserDtoMapper.class);

    @Test
    void toUserSummaryDto_shouldMapIdToUserId() {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(105L);
        entity.setEmail("admin@example.edu.vn");

        UserSummaryDto dto = mapper.toUserSummaryDto(entity);

        assertNotNull(dto);
        assertEquals(105L, dto.userId());
        assertEquals("admin@example.edu.vn", dto.email());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl ddd-application -Dtest=UserDtoMapperTest`  
Expected: FAIL (`AssertionFailedError: expected: <105> but was: <null>`).

- [ ] **Step 3: Implement mapping in `UserDtoMapper.java`**

In `ddd-application/src/main/java/com/ddd/application/mapper/UserDtoMapper.java`, add `@Mapping(target = "userId", source = "id")`:
```java
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "roles", expression = "java(mapRoleNames(userJpaEntity.getRoles()))")
    @Mapping(target = "role", expression = "java(mapPrimaryRole(userJpaEntity.getRoles()))")
    UserSummaryDto toUserSummaryDto(UserJpaEntity userJpaEntity);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-application -Dtest=UserDtoMapperTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-application/src/main/java/com/ddd/application/mapper/UserDtoMapper.java \
        ddd-application/src/test/java/com/ddd/application/mapper/UserDtoMapperTest.java
git commit -m "fix(user): map UserJpaEntity id to UserSummaryDto userId"
```

---

### Task 2: Add `eventTypeId` and `criteriaId` to Event DTOs and Mappers

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java:47-53`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/event/impl/StoredEventQueryServiceImplTest.java`
- Test: `ddd-application/src/test/java/com/ddd/application/mapper/EventDtoMapperTest.java`

**Interfaces:**
- Consumes: `EventJpaEntity.getEventType().getId()`, `EventJpaEntity.getCriteria().getId()`
- Produces: `EventInfoDto.eventTypeId()`, `EventInfoDto.criteriaId()`, `EventResponseDto.eventTypeId()`, `EventResponseDto.criteriaId()`

- [ ] **Step 1: Write the failing unit test**

Create `ddd-application/src/test/java/com/ddd/application/mapper/EventDtoMapperTest.java`:
```java
package com.ddd.application.mapper;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventTypeJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventDtoMapperTest {
    private final EventDtoMapper mapper = Mappers.getMapper(EventDtoMapper.class);

    @Test
    void toInfoDto_shouldMapEventTypeIdAndCriteriaId() {
        EventTypeJpaEntity type = new EventTypeJpaEntity();
        type.setId(20L);
        type.setName("Workshop");

        CriteriaJpaEntity criteria = new CriteriaJpaEntity();
        criteria.setId(55L);
        criteria.setName("Volunteer");

        EventJpaEntity event = new EventJpaEntity();
        event.setId(10L);
        event.setName("Tech Conference");
        event.setEventType(type);
        event.setCriteria(criteria);

        EventInfoDto dto = mapper.toInfoDto(event);

        assertNotNull(dto);
        assertEquals(20L, dto.eventTypeId());
        assertEquals("Workshop", dto.eventTypeName());
        assertEquals(55L, dto.criteriaId());
        assertEquals("Volunteer", dto.criteriaName());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl ddd-application -Dtest=EventDtoMapperTest`  
Expected: FAIL (Compilation error or missing getters on `EventInfoDto`).

- [ ] **Step 3: Update `EventInfoDto`, `EventResponseDto`, and `EventDtoMapper`**

1. In `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`:
   - Add `Long eventTypeId` and `Long criteriaId` to the record header.
   - Update secondary constructors to preserve backwards compatibility.
2. In `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`:
   - Add `Long eventTypeId` and `Long criteriaId` to the record header.
   - Update secondary constructors to preserve backwards compatibility.
3. In `ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java`:
   ```java
   @Mapping(target = "eventTypeId", source = "eventType.id")
   @Mapping(target = "eventTypeName", source = "eventType.name")
   @Mapping(target = "criteriaId", source = "criteria.id")
   @Mapping(target = "criteriaName", source = "criteria.name")
   EventInfoDto toInfoDto(EventJpaEntity entity);
   ```
4. In `ddd-application/src/test/java/com/ddd/application/service/event/impl/StoredEventQueryServiceImplTest.java`:
   - Update mock instantiations of `new EventInfoDto(...)` with new parameters.

- [ ] **Step 4: Run tests to verify compilation and passing**

Run: `./mvnw clean test -pl ddd-application,ddd-api -Dtest=EventDtoMapperTest,StoredEventQueryServiceImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java \
        ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java \
        ddd-application/src/main/java/com/ddd/application/mapper/EventDtoMapper.java \
        ddd-application/src/test/java/com/ddd/application/mapper/EventDtoMapperTest.java \
        ddd-application/src/test/java/com/ddd/application/service/event/impl/StoredEventQueryServiceImplTest.java
git commit -m "feat(event): expose eventTypeId and criteriaId in EventInfoDto and EventResponseDto"
```

---

### Task 3: Fix SV5T Criteria Transaction Rollback in `markUncompleted`

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java:241-255`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaTransactionCommandServiceImpl.java:103-117`
- Test: `ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImplTest.java`

**Interfaces:**
- Consumes: `StudentCriteriaProgressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(Long, Long, Long)`
- Produces: Idempotent return without exception when progress record is absent

- [ ] **Step 1: Write unit test reproducing the missing progress scenario**

In `ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImplTest.java`:
```java
@Test
void markUncompleted_whenProgressNotFound_shouldReturnSafelyWithoutException() {
    when(progressRepository.findByUserIdAndCriteriaIdAndAcademicYearId(136L, 101L, 2L))
            .thenReturn(Optional.empty());

    assertDoesNotThrow(() -> 
        studentCriteriaProgressCommandService.markUncompleted(136L, 101L, 2L)
    );

    verify(progressRepository, never()).delete(any());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl ddd-application -Dtest=StudentCriteriaProgressCommandServiceImplTest#markUncompleted_whenProgressNotFound_shouldReturnSafelyWithoutException`  
Expected: FAIL (`IllegalArgumentException: No progress found for user: 136...`).

- [ ] **Step 3: Implement idempotent check in `StudentCriteriaProgressCommandServiceImpl.java`**

Replace `orElseThrow(...)` with `isEmpty()` early return:
```java
@Override
public void markUncompleted(Long userId, Long criteriaId, Long academicYearId) {
    var progressOpt = progressRepository
            .findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId);

    if (progressOpt.isEmpty()) {
        log.info("No progress found for user: {}, criteria: {}, academic year: {}, skipping uncomplete",
                userId, criteriaId, academicYearId);
        return;
    }

    StudentCriteriaProgress leafProgress = progressOpt.get();
    if (!Boolean.TRUE.equals(leafProgress.getIsCompleted())) {
        return;
    }
    // ... remaining hierarchy tree recalculation & delete ...
}
```

In `StudentCriteriaTransactionCommandServiceImpl.deleteTransaction`:
Wrap the call to `criteriaProgressCommandService.markUncompleted(...)` in a `try-catch (Exception e)` logging warning, ensuring transaction deletion is always committed.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-application -Dtest=StudentCriteriaProgressCommandServiceImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImpl.java \
        ddd-application/src/main/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaTransactionCommandServiceImpl.java \
        ddd-application/src/test/java/com/ddd/application/service/criteriaprogress/impl/StudentCriteriaProgressCommandServiceImplTest.java
git commit -m "fix(criteria): make markUncompleted idempotent when progress record does not exist"
```

---

### Task 4: Fix `RegistrationJpaRepository` Composite Key Type & Repository Optimizations

**Files:**
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java:17`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java:37-100`
- Test: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/RegistrationRepositoryImplTest.java`

**Interfaces:**
- Consumes: `RegistrationJpaEntity.getId() -> RegistrationId`
- Produces: `RegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, RegistrationId>`

- [ ] **Step 1: Write test for composite key lookup and update**

Create `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/RegistrationRepositoryImplTest.java`:
```java
package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Registration;
import com.ddd.infrastructure.entity.RegistrationId;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationRepositoryImplTest {
    @Mock private RegistrationJpaRepository registrationJpaRepository;
    @Mock private RegistrationMapper registrationMapper;
    @Mock private UserJpaRepository userJpaRepository;
    @Mock private EventJpaRepository eventJpaRepository;

    @InjectMocks private RegistrationRepositoryImpl repository;

    @Test
    void save_whenAlreadyExists_shouldNotQueryUserOrEvent() {
        Registration reg = new Registration();
        RegistrationJpaEntity existing = new RegistrationJpaEntity();
        existing.setId(new RegistrationId(1L, 2L));

        when(registrationJpaRepository.findById(new RegistrationId(1L, 2L)))
                .thenReturn(Optional.of(existing));

        repository.save(reg, 1L, 2L);

        verify(registrationJpaRepository).save(existing);
        verifyNoInteractions(userJpaRepository);
        verifyNoInteractions(eventJpaRepository);
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw test -pl ddd-infrastructure -Dtest=RegistrationRepositoryImplTest`  
Expected: FAIL (Method does not use `findById(RegistrationId)` or `RegistrationJpaRepository` signature mismatches).

- [ ] **Step 3: Update `RegistrationJpaRepository` and `RegistrationRepositoryImpl`**

1. In `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java`:
   Change declaration to:
   ```java
   public interface RegistrationJpaRepository extends JpaRepository<RegistrationJpaEntity, RegistrationId>
   ```
2. In `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java`:
   Optimize `save`:
   ```java
   @Override
   public void save(Registration registration, Long userId, Long eventId) {
       RegistrationId regId = new RegistrationId(userId, eventId);
       RegistrationJpaEntity entity = registrationJpaRepository.findById(regId)
               .orElseGet(() -> {
                   UserJpaEntity user = userJpaRepository.findById(userId)
                           .orElseThrow(() -> new ResourceNotFoundException("Not found user"));
                   EventJpaEntity event = eventJpaRepository.findById(eventId)
                           .orElseThrow(() -> new ResourceNotFoundException("Not found Event"));
                   RegistrationJpaEntity newEntity = new RegistrationJpaEntity();
                   newEntity.setId(regId);
                   newEntity.setUser(user);
                   newEntity.setEvent(event);
                   return newEntity;
               });

       registrationMapper.updateEntityFromDomain(registration, entity);
       registrationJpaRepository.save(entity);
   }
   ```
   Optimize `saveAll`:
   Filter `existingEntities` safely against null keys and use `e.getId().getUserId()` to avoid lazy proxy initializations.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-infrastructure -Dtest=RegistrationRepositoryImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java \
        ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java \
        ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/RegistrationRepositoryImplTest.java
git commit -m "refactor(registration): fix RegistrationJpaRepository id type to RegistrationId and eliminate redundant queries"
```

---

### Task 5: Allow Superadmin Member Lookup on Inactive Fanpages & Expose Staff Profile

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImpl.java:48-55`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/fanpage/FanpageMemberDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/fanpage/res/FanpageMemberResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/mapper/FanpageMemberDtoMapper.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImplTest.java`

**Interfaces:**
- Consumes: `SecurityContextPort.hasRole("ADMIN")`, `UserJpaEntity.getFanpageAdminProfile()`
- Produces: `FanpageMemberDto(..., String staffId, String phoneNumber)`

- [ ] **Step 1: Write test verifying admin can read members of inactive fanpage**

In `ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImplTest.java`:
```java
@Test
void getMembersOfFanpage_whenFanpageInactiveAndUserIsAdmin_shouldReturnMembers() {
    when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
    when(fanpageRepository.findById(99L)).thenReturn(Optional.of(new Fanpage()));
    when(fanpageMemberJpaRepository.findAllByFanpageId(99L)).thenReturn(List.of(new FanpageMemberJpaEntity()));

    List<FanpageMemberDto> members = fanpageQueryService.getMembersOfFanpage(99L);

    assertNotNull(members);
    assertEquals(1, members.size());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -pl ddd-application -Dtest=FanpageQueryServiceImplTest`  
Expected: FAIL (Calls `findByIdAndIsActive(99L, true)` or throws 404).

- [ ] **Step 3: Implement Superadmin bypass and staff profile mapping**

1. In `FanpageQueryServiceImpl.java`:
   ```java
   @Override
   public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
       boolean isAdmin = securityContextPort.hasRole("ADMIN");
       var fanpageOpt = isAdmin 
               ? fanpageRepository.findById(fanpageId)
               : fanpageRepository.findByIdAndIsActive(fanpageId, true);

       if (fanpageOpt.isEmpty()) {
           throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
       }

       List<FanpageMemberJpaEntity> members = isAdmin
               ? fanpageMemberJpaRepository.findAllByFanpageId(fanpageId)
               : fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);

       return members.stream().map(fanpageMemberDtoMapper::toDto).toList();
   }
   ```
2. In `FanpageMemberDto.java` and `FanpageMemberResponseDto.java`, add:
   ```java
   String staffId,
   String phoneNumber
   ```
3. In `FanpageMemberDtoMapper.java`:
   Map `entity.user.fanpageAdminProfile.staffId -> staffId` and `entity.user.fanpageAdminProfile.phoneNumber -> phoneNumber`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-application -Dtest=FanpageQueryServiceImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImpl.java \
        ddd-application/src/main/java/com/ddd/application/dto/fanpage/FanpageMemberDto.java \
        ddd-api/src/main/java/com/ddd/api/dto/fanpage/res/FanpageMemberResponseDto.java \
        ddd-application/src/main/java/com/ddd/application/mapper/FanpageMemberDtoMapper.java \
        ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageQueryServiceImplTest.java
git commit -m "feat(fanpage): allow admin member lookup on inactive fanpages and expose staff profile"
```

---

### Task 6: Add Batch `registeredCount` to Event Responses

**Files:**
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventQueryServiceImplTest.java`

**Interfaces:**
- Consumes: `RegistrationJpaRepository.countRegistrationsByEventIds(List<Long>, RegistrationStatusEnum)`
- Produces: `EventInfoDto(..., Long registeredCount)`

- [ ] **Step 1: Write test verifying batch registration count hydration**

In `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventQueryServiceImplTest.java`:
```java
@Test
void searchAndFilterEvents_shouldPopulateRegisteredCount() {
    EventJpaEntity entity = new EventJpaEntity();
    entity.setId(10L);
    when(eventJpaRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));
    when(registrationJpaRepository.countActiveRegistrationsByEventIds(List.of(10L)))
            .thenReturn(List.of(new EventRegistrationCountView(10L, 42L)));

    Page<EventInfoDto> result = eventQueryService.searchAndFilterEvents(criteria);

    assertEquals(42L, result.getContent().get(0).registeredCount());
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw test -pl ddd-application -Dtest=EventQueryServiceImplTest`  
Expected: FAIL (Method does not exist or `registeredCount` is missing).

- [ ] **Step 3: Implement batch registration counting**

1. In `RegistrationJpaRepository.java`:
   ```java
   @Query("SELECT r.event.id AS eventId, COUNT(r.id) AS count " +
          "FROM RegistrationJpaEntity r " +
          "WHERE r.event.id IN :eventIds AND r.status <> com.ddd.domain.enums.RegistrationStatusEnum.CANCELLED " +
          "GROUP BY r.event.id")
   List<EventRegistrationCountView> countActiveRegistrationsByEventIds(@Param("eventIds") Collection<Long> eventIds);
   ```
2. In `EventInfoDto.java` and `EventResponseDto.java`, add `Long registeredCount` (defaulting to 0 if null).
3. In `EventQueryServiceImpl.searchAndFilterEvents`:
   Extract IDs from the paged result, execute the single batch count query, and hydrate `registeredCount` into each `EventInfoDto`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-application -Dtest=EventQueryServiceImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/RegistrationJpaRepository.java \
        ddd-application/src/main/java/com/ddd/application/dto/event/EventInfoDto.java \
        ddd-api/src/main/java/com/ddd/api/dto/event/res/EventResponseDto.java \
        ddd-application/src/main/java/com/ddd/application/service/event/impl/EventQueryServiceImpl.java \
        ddd-application/src/test/java/com/ddd/application/service/event/impl/EventQueryServiceImplTest.java
git commit -m "feat(event): batch compute and expose registeredCount in event responses"
```

---

### Task 7: Superadmin Fanpage Rejection with Reason

**Files:**
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/FanpageJpaEntity.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/model/Fanpage.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/fanpage/FanpageCommandService.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImpl.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/controller/FanpageController.java`
- Test: `ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImplTest.java`

**Interfaces:**
- Consumes: `POST /fanpages/admin/reject-fanpage/{fanpageId}` with body `{ "reason": "Invalid documentation" }`
- Produces: Fanpage with `status = REJECTED` and `rejectionReason` saved

- [ ] **Step 1: Write unit test for fanpage rejection**

In `ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImplTest.java`:
```java
@Test
void rejectFanpage_shouldSetStatusToRejectedAndStoreReason() {
    Fanpage fanpage = new Fanpage();
    fanpage.setId(5L);
    when(fanpageRepository.findById(5L)).thenReturn(Optional.of(fanpage));

    fanpageCommandService.rejectFanpage(5L, "Missing proof of organization");

    assertEquals("REJECTED", fanpage.getStatus());
    assertEquals("Missing proof of organization", fanpage.getRejectionReason());
    verify(fanpageRepository).save(fanpage);
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./mvnw test -pl ddd-application -Dtest=FanpageCommandServiceImplTest#rejectFanpage_shouldSetStatusToRejectedAndStoreReason`  
Expected: FAIL (`rejectFanpage` method not found).

- [ ] **Step 3: Implement `rejectFanpage`**

1. Add `rejectionReason` column to `Fanpage` and `FanpageJpaEntity`.
2. Add `rejectFanpage(Long fanpageId, String reason)` to `FanpageCommandService` and implement in `FanpageCommandServiceImpl`.
3. In `FanpageController`:
   ```java
   @PostMapping("/admin/reject-fanpage/{fanpageId}")
   public BaseResponse<?> rejectFanpage(@PathVariable Long fanpageId, @RequestBody @Valid RejectFanpageRequestDto request) {
       fanpageCommandService.rejectFanpage(fanpageId, request.reason());
       return BaseResponse.ok();
   }
   ```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -pl ddd-application -Dtest=FanpageCommandServiceImplTest`  
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add ddd-domain/src/main/java/com/ddd/domain/model/Fanpage.java \
        ddd-infrastructure/src/main/java/com/ddd/infrastructure/entity/FanpageJpaEntity.java \
        ddd-application/src/main/java/com/ddd/application/service/fanpage/FanpageCommandService.java \
        ddd-application/src/main/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImpl.java \
        ddd-api/src/main/java/com/ddd/api/controller/FanpageController.java \
        ddd-application/src/test/java/com/ddd/application/service/fanpage/impl/FanpageCommandServiceImplTest.java
git commit -m "feat(fanpage): add superadmin reject-fanpage endpoint with reason"
```

---

## Plan Review & Verification Checklist
1. **Spec Coverage**: All 4 documents in `docs/bug/` are covered across the 7 tasks.
2. **Step Scan**: Every step specifies exact file paths, method signatures, test assertions, and terminal verification commands.
3. **Type Consistency**: `RegistrationId`, `Long criteriaId`, `Long eventTypeId`, and `Long registeredCount` are consistent between models and mappers.
4. **Clean Architecture**: Domain layer remains untouched by infrastructure dependencies.
