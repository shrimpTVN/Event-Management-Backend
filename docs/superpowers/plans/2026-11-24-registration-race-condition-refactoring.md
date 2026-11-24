# Registration Race Condition Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. This document is a proposed plan; writing it does not authorize implementation.

**Goal:** Ensure concurrent registration requests cannot allocate more places than an event's configured capacity, including competition for the final slot.

**Architecture:** Use the existing event row as a pessimistic locking coordination point. In one transaction, acquire the event lock, validate eligibility, count registrations occupying places, and insert or reactivate a registration. Cancellation, event edits, and registration status writers follow the same event-first locking order.

**Tech Stack:** Existing Spring Boot 4.1.1, Spring Data JPA/Hibernate, PostgreSQL, Maven, JUnit 5, and Mockito. Use the repository's existing Java build configuration.

**Spec:** The proposed design and acceptance criteria embedded below, based on the registration discussion and the current repository. No separate approved spec exists; implementation awaits review of this document.

## Global Constraints

- `events.capacity` remains the fanpage-configured maximum. Registration and cancellation never increment or decrement it.
- Occupied places are `COUNT(registrations WHERE event_id = ? AND status <> 'CANCELLED')`, matching the existing count query. Zero capacity means no places; it does not mean unlimited.
- Do not add `registered_count`, `remaining_slots`, `@Version`, Redis locks, or a queue in this refactor.
- Keep `PRIMARY KEY (user_id, event_id)` and reuse the existing row for a cancelled student's re-registration.
- All participating writes lock the event before reading or writing registrations. The lock and subsequent operations share one connection and transaction.
- Use `READ_COMMITTED`. Count in a separate statement after the event lock is acquired, so a waiter sees the preceding transaction's committed registration.
- No automatic retries inside an aborted transaction. A full event is a business rejection; a lock failure is retryable by a new request.
- Changes enforce total capacity only. Male/female quantities retain existing behavior; gender quotas are outside this plan.
- Do not execute product changes, commits, or migrations as part of writing this plan.

## Review Focus

- Same-student duplicate requests: one row and one successful registration, with a controlled duplicate response for the other request (Tasks 2 and 5).
- Cancelled registrations: re-registration consumes a place, and attendance actions cannot silently turn a cancelled row into an occupied place (Tasks 2, 3, and 5).
- Capacity reduction: reject a new capacity below occupied places, including when registration is concurrent (Tasks 3 and 5).
- Waiting requests: recheck event dates/status after acquiring the lock; closing or deactivating an event prevents subsequent registrations (Tasks 2, 3, and 5).
- Rollback and timeout: failed transactions release the lock and leave no registration; contention returns a controlled response rather than claiming the event is full (Tasks 4 and 5).

## Proposed design and acceptance criteria

### Current flow

`RegistrationController.registerEvent()` calls `RegistrationCommandServiceImpl.registerEvent()`. The service has Jakarta `@Transactional`; it reads the event, validates dates/status and the student profile, checks duplicates, then saves. There is no capacity check. `RegistrationRepositoryImpl.save()` finds or creates the student's composite-key registration row.

`RegistrationJpaRepository.countActiveRegistrationsByEventId(Long eventId)` already counts every status except `CANCELLED`, but the domain repository does not expose it. `EventRepositoryImpl.findById()` currently uses an ordinary, unlocked query. Event edits and cancellation also read without a lock.

### Why the event row

Competing students insert different registration rows. The event is the common existing row they can all lock, even when no registration exists yet. The event's values need not change. The lock does not itself forbid registration inserts: correctness depends on every application writer following this protocol.

Ordinary event reads remain available. Conflicting row locks and writes wait until transaction completion. Use a dedicated locking repository method rather than adding locks to normal event reads. See [PostgreSQL row locks](https://www.postgresql.org/docs/current/explicit-locking.html) and [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html).

### Registration transaction

1. Acquire the event row lock; obtain the current event state without first loading the event through an ordinary read in this transaction.
2. Check active state, published status, and registration dates using the time after lock acquisition.
3. Preserve the existing user/profile validation and duplicate behavior. Check duplicates before capacity so a repeated request retains its duplicate response even when full.
4. Count occupied places. If count is greater than or equal to capacity, throw `EventFullException` without saving.
5. Create a new registration or reuse the `CANCELLED` row, preserving existing field behavior; set `REGISTERED` and `regDate`, then save.
6. Commit. A save or commit failure rolls back the registration and releases the lock.

Use the event-first order already implied by the service to keep this refactor small. Keep external network calls out of the locked transaction. If a request joins an outer transaction, that caller must also use `READ_COMMITTED` and avoid preloading event/registration state; inspect callers before implementation.

### Winner and fairness

With capacity 100 and 99 occupied places, A acquires the lock, counts 99, saves, and commits. B then acquires the lock, counts 100, and receives a full response. If A rolls back, B can succeed. The winner is the eligible transaction that claims the place and commits successfully; database lock acquisition is not a promise of browser-click or strict FIFO order. `regDate` is not a commit-order audit log.

### Alternatives considered

- **Pessimistic locking — selected:** fits the current capacity-plus-count model and needs no event counter/version migration. Contention serializes writers for one event.
- **Optimistic locking — deferred:** would require a version on the shared event plus an actual or forced version increment for every capacity-affecting operation, and whole-transaction retries. Merely adding `@Version` while inserting registration rows would not detect the conflict.

Success means simultaneous valid attempts at the last place produce exactly one committed registration, total occupied places never increase beyond capacity, and cancellation/re-registration and capacity edits preserve that invariant. Existing over-capacity data is reported and blocks new registrations; do not silently cancel existing students.

## Task 1: Add event locking and domain count operations

**Files:**
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/EventRepository.java`
- Modify: `ddd-domain/src/main/java/com/ddd/domain/repository/RegistrationRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/jpaRepository/EventJpaRepository.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/EventRepositoryImpl.java`
- Modify: `ddd-infrastructure/src/main/java/com/ddd/infrastructure/repository/RegistrationRepositoryImpl.java`
- Modify: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/RegistrationRepositoryImplTest.java`
- Create: `ddd-infrastructure/src/test/java/com/ddd/infrastructure/repository/EventRepositoryImplTest.java`

**Interfaces:**
- Produces: `Event EventRepository.findByIdForUpdate(Long id)`; throws the existing `ResourceNotFoundException` when absent.
- Produces: `Optional<EventJpaEntity> EventJpaRepository.findByIdForUpdate(Long id)`.
- Produces: `long RegistrationRepository.countActiveRegistrationsByEventId(Long eventId)`; delegates to the existing JPA count.

- [ ] Write adapter tests for missing-event behavior and count delegation, including zero. Run them and confirm failure because the new methods are missing.
- [ ] Add a dedicated JPQL event-by-ID query with `@Lock(LockModeType.PESSIMISTIC_WRITE)` and no fetch joins/entity graph. Map the managed event to the domain model in the adapter; do not call `save(event)` merely to lock it.
- [ ] In the adapter's locking operation, require an active transaction and use PostgreSQL `select set_config('lock_timeout', '3s', true)` on the same connection before the locking query. This bounds waits per lock acquisition and resets at transaction end; do not rely only on a provider timeout hint. The real database test in Task 5 must verify the bound and translation.
- [ ] Expose count through the domain adapter, leaving the existing non-cancelled query unchanged. Preserve the ordinary `findById` behavior for queries.
- [ ] Run `./mvnw -pl ddd-infrastructure -am -Dtest=EventRepositoryImplTest,RegistrationRepositoryImplTest -Dsurefire.failIfNoSpecifiedTests=false test`. Expected: both test classes pass. Real lock behavior is verified in Task 5, not by mocks.
- [ ] Review and commit this repository change separately during implementation.

## Task 2: Make registration and cancellation capacity-safe

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`
- Create: `ddd-domain/src/main/java/com/ddd/domain/exception/EventFullException.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImplTest.java`

**Interfaces:**
- Consumes: Task 1 repository methods.
- Preserves: `void registerEvent(Long eventId)` and `void cancelRegistration(Long eventId, String reason)`.
- Produces: `EventFullException extends EventRegistrationException`, with a no-argument constructor and message `Event is full.`

- [ ] Add behavioral tests: capacity 100/count 99 saves; count 100 or 101 rejects with `EventFullException` and no save; capacity 0/count 0 rejects; capacity remains unchanged in every case.
- [ ] Add tests for a cancelled student's re-registration with available space and when full; retain duplicate rejection for every non-cancelled registration. Preserve profile, user-not-found, date, and status validation tests, and add inactive-event rejection.
- [ ] Run the service test class and confirm the new full-event tests fail against the existing implementation.
- [ ] Replace the service's Jakarta transaction annotation with Spring `@Transactional(isolation = Isolation.READ_COMMITTED)` at class level. Keep existing public methods transactional and runtime exceptions rolling back. Inspect actual callers for an incompatible outer transaction; do not add `REQUIRES_NEW` to hide one.
- [ ] Implement the registration sequence from the design. Use `findByIdForUpdate()` as the first event read and count only after the lock and duplicate check. Do not use a cached UI count or call a query service for the capacity decision.
- [ ] Make cancellation acquire that same event lock before reading the registration or validating its current state; preserve its existing deadline/status rules. A repeated cancellation must not release another place.
- [ ] Run `./mvnw -pl ddd-application -am -Dtest=RegistrationCommandServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`. Expected: new cases and existing service tests pass; update fixtures to set realistic capacities explicitly.
- [ ] Review and commit this service change separately during implementation.

## Task 3: Coordinate related event and registration writers

**Files:**
- Modify: `ddd-application/src/main/java/com/ddd/application/service/event/impl/EventCommandServiceImpl.java`
- Modify: `ddd-application/src/main/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImpl.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/event/impl/EventCapacityConcurrencyPolicyTest.java`
- Modify: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationCommandServiceImplTest.java`

**Interfaces:**
- Consumes: `findByIdForUpdate(Long id)` and `countActiveRegistrationsByEventId(Long eventId)`.
- Preserves: existing controller and command service signatures.

- [ ] Add tests: reducing capacity below occupied places fails without saving; equal or greater capacity is allowed subject to existing validations. Use `BusinessException("Event capacity cannot be lower than current registrations.")` for the lower-capacity case.
- [ ] Add tests that cancelled registrations remain cancelled during event completion and cannot be marked absent or checked out by attendance commands. Attendance must never allocate a place by promoting a cancelled row.
- [ ] Run the affected tests and confirm the new cases expose the existing gaps.
- [ ] In `updateEvent`, `changeEventStatus`, and `deleteEvent`, acquire the event lock before mapping or changing event state. Use Spring `@Transactional(isolation = Isolation.READ_COMMITTED)` on these methods. Add `RegistrationRepository` to the event command service and reject a proposed capacity below the locked count before saving.
- [ ] In registration status writers (`checkIn`, `checkOut`, `fanpageMemberCheckIn`, `fanpageMemberCheckOut`, `fanpageMemberBatchAbsent`, and `fanpageMemberMarkEventAsCompleted`), use the same locking event read before fetching registrations. Reject cancelled rows in attendance operations and skip them when completing the event. Preserve all other existing state rules.
- [ ] Use CodeGraph to inspect all callers of registration `save`/`saveAll` and all event update paths. Any additional writer that can create/reactivate a place or change registration status must join this event-first protocol; record its exact path in the implementation review. Avoid an unrelated service restructure.
- [ ] Run `./mvnw -pl ddd-application -am -Dtest=RegistrationCommandServiceImplTest,EventCapacityConcurrencyPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test`. Expected: all cases pass. The known completion bug that promotes cancelled rows must be covered.
- [ ] Review and commit these coordinated writer changes separately during implementation.

## Task 4: Return explicit full and contention responses

**Files:**
- Modify: `ddd-api/src/main/java/com/ddd/api/common/enums/ErrorCodeEnum.java`
- Modify: `ddd-api/src/main/java/com/ddd/api/exception/GlobalExceptionHandler.java`
- Create: `ddd-api/src/test/java/com/ddd/api/exception/RegistrationConcurrencyExceptionHandlerTest.java`
- Modify: `ddd-api/src/test/java/com/ddd/api/controller/RegistrationControllerTest.java`

**Interfaces:**
- Consumes: `EventFullException` and translated Spring pessimistic-lock failures.
- Produces: HTTP 409, code `EVENT.004`, message `Event is full.` for capacity exhaustion.
- Produces: HTTP 409, existing code `005` (`CONCURRENT_MODIFICATION`), message `Registration is busy. Please try again.` for registration endpoint lock failures; use a generic retry message on other endpoints.

- [ ] Add handler tests for both responses and unchanged duplicate/profile/event-date errors. Add controller/advice coverage proving a failed registration does not return the success envelope.
- [ ] Run the new tests and confirm the missing mappings fail.
- [ ] Add `EVENT_FULL` and a specific `EventFullException` handler. Keep ordinary `EventRegistrationException` responses unchanged.
- [ ] Handle Spring `PessimisticLockingFailureException` (including translated cannot-acquire-lock/deadlock subclasses) as a conflict. Verify actual exceptions in Task 5; add a narrower persistence exception handler only if the provider demonstrably exposes an untranslated lock exception. Do not classify all database exceptions as contention or full.
- [ ] Return the response after the service transaction has unwound. Do not catch a lock failure inside the transaction and continue querying or inserting. Do not implement automatic HTTP retries.
- [ ] Run `./mvnw -pl ddd-api -am -Dtest=RegistrationConcurrencyExceptionHandlerTest,RegistrationControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`. Expected: both classes pass.
- [ ] Review and commit the API behavior separately during implementation.

## Task 5: Prove correctness against PostgreSQL

**Files:**
- Create: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationConcurrencyIntegrationTest.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationPostgresTestConfig.java`
- Create: `ddd-application/src/test/java/com/ddd/application/service/registration/impl/RegistrationPostgresFixtures.java`

**Interfaces:**
- Consumes: real Spring-proxied command services, JPA repositories/mappers, Hibernate and PostgreSQL transactions from Tasks 1–4.
- Test database settings: `REGISTRATION_TEST_JDBC_URL`, `REGISTRATION_TEST_DB_USER`, and `REGISTRATION_TEST_DB_PASSWORD`; system properties of the same names take precedence over environment values.
- Configuration produces a `DataSource`, JPA `EntityManagerFactory`, `JpaTransactionManager`, actual event/registration adapters and services, and a thread-local test `SecurityContextPort`. Stub unrelated notification/points ports only; do not stub the event lock, count or save.
- Fixtures produce valid user/profile/event dependencies from `database/schema.sql` and required migrations in a uniquely named isolated test schema; they also clean it up after the test suite. No writes to the application's ordinary schema.

- [ ] Start by writing the final-slot test; establish its setup and assertion work against the old flow before applying the fix when practical. Seed capacity 2 with one occupied place, release 10 distinct eligible students from a barrier, and invoke the real service in separate worker transactions/connections. Assert exactly one success, nine full rejections, and final occupied count 2 from an independent connection after all workers finish.
- [ ] Use latches at transaction boundaries for two-request ordering tests. No arbitrary sleeps, no global test transaction, and no assertion about which student wins an uncontrolled race. Verify pool size is sufficient for all workers and bound every future/latch wait.
- [ ] Add a controlled winner test: A holds the event lock, B attempts registration, A registers and commits, B rejects as full. Add its rollback variant: A inserts then rolls back, B succeeds, and only B is visible afterward.
- [ ] Add concurrent requests for the same student, including simultaneous reactivation of one cancelled row. Assert one success, one controlled duplicate, and only one occupied row.
- [ ] Add cancellation versus registration with both forced lock orders: cancellation first makes a place available; registration first sees the full event and can retry only after cancellation commits. Assert counts and statuses, not just exceptions.
- [ ] Add capacity reduction versus registration with both forced lock orders: either registration commits and reduction below the new count rejects, or reduction commits and registration rejects. The final count must never exceed final capacity.
- [ ] Add closure/deactivation while B waits: B revalidates and rejects after acquiring the lock. Add an event whose registration deadline passes while B waits, with timing controlled within the 3-second lock bound.
- [ ] Add different-event independence: hold A's event lock and prove a registration for event B commits before releasing A. A normal event read must also remain possible from another connection.
- [ ] Hold a lock beyond 3 seconds and assert a contender fails within a bounded test deadline of 8 seconds, translates to the handled lock exception, and leaves no registration. Roll back/release the holder and verify a new request succeeds. A timeout is not `EventFullException`.
- [ ] Fail immediately with a clear configuration error if the explicit PostgreSQL test is invoked without database settings. Do not silently skip the concurrency suite or substitute H2/mocks. Reuse existing dependencies; no Testcontainers dependency is required.
- [ ] Run `./mvnw -pl ddd-application -am -Dtest=RegistrationConcurrencyIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` with the isolated PostgreSQL settings supplied securely. Expected: all integration cases pass, no over-capacity rows, and no leftover test schema.
- [ ] Run the focused unit/API commands from Tasks 1–4 and then `./mvnw test` with existing database-backed suite prerequisites configured. Record unrelated baseline failures separately; never claim database correctness from unit tests alone.
- [ ] Review and commit concurrency coverage separately during implementation.

## Release checks and limitations

- Before rollout, inspect existing over-capacity events with the same non-cancelled count. Report them for fanpage resolution; do not auto-cancel registrations. A pre-existing violation is not repaired by this refactor.
- Deploy the coordinated writers together. Old instances that still insert without acquiring the event lock can bypass the rule; drain those instances before accepting registrations under the new guarantee.
- The composite registration key already supplies duplicate protection; no new unique constraint or capacity-column migration is required. Count-query indexing is a later measured optimization.
- Observe lock contention and rejected registration errors using existing logs; log event ID and outcome without student profile details. Event-level locking also serializes attendance batches, so keep batches and transactions bounded and reassess if measured contention warrants a different design.
- Direct SQL writers must honor the protocol. If writes outside these services must be supported, database-enforced capacity allocation needs a separate design.
- Acceptance: one final-slot winner, count never above capacity after successful participating writes from a valid initial state, unchanged configured capacity, controlled full/duplicate/contention outcomes, and passing real PostgreSQL concurrency coverage.

## Review handoff

Review the pessimistic-lock choice, the non-cancelled occupancy rule, the proposed 3-second lock wait bound, and HTTP 409 responses before implementation. Execution can remain inline in this chat using `executing-plans`; no execution work has been started.
