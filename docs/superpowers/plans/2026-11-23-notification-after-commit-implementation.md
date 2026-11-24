# Notification Delivery After Commit Implementation Plan

> **For agentic workers:** Use the repository's `executing-plans` skill when implementing this plan. Track the deliverables below; a separate checklist for every coding action is unnecessary.

**Goal:** Send real-time notification pushes only after the notification transaction commits successfully, for both single and batch creation.

**Architecture:** Keep persistence in `NotificationCommandServiceImpl` and move transaction-aware push scheduling into a small `NotificationDispatchAfterCommit` component using Spring transaction synchronization and the existing `NotificationPushPort`.

**Tech Stack:** Existing Spring transactions, JPA, PostgreSQL, JUnit, and Mockito; no new runtime dependencies.

**Spec:** [Issue report, C.2](../../bug/backend-account-security-and-staff-profile-request.md) and [parent plan, Task 2](2026-11-23-account-security-staff-profile-refactor.md).

## Scope and transaction contract

Both creation methods currently push before their `REQUIRES_NEW` transaction commits. Saving or flushing successfully is insufficient: commit can still fail after clients receive the push.

Retain `REQUIRES_NEW` for both methods. A batch commits or rolls back as one notification transaction, independently of its caller. If that transaction commits and an outer caller subsequently rolls back, the saved notifications and their pushes remain valid under this existing behavior. Do not change caller/notification transaction ownership in this task.

Keep public service signatures, response DTOs, recipient selection, notification contents, and the API contract unchanged. Schema repair belongs to Task 1; durable delivery, retries, outbox infrastructure, and async redesign are outside this task. After-commit dispatch provides best-effort delivery, not a guarantee against process failure after persistence.

## Files

- Modify `ddd-application/src/main/java/com/ddd/application/service/notification/impl/NotificationCommandServiceImpl.java`.
- Create `ddd-application/src/main/java/com/ddd/application/service/notification/NotificationDispatchAfterCommit.java`.
- Update `ddd-application/src/test/java/com/ddd/application/service/notification/impl/NotificationCommandServiceImplTest.java`.
- Create `ddd-application/src/test/java/com/ddd/application/service/notification/NotificationDispatchAfterCommitTest.java`.
- Create `ddd-application/src/test/java/com/ddd/application/service/notification/impl/NotificationTransactionIntegrationTest.java`.

## Implementation

- [ ] **Add the after-commit dispatcher.** Provide `dispatchSingleAfterCommit(Notification notification)` and `dispatchAfterCommit(List<Notification> notifications)`. Register callbacks only while an actual transaction and synchronization are active; reject calls outside that boundary rather than pushing immediately. Capture detached value snapshots before registration so later mutation or lazy persistence access cannot affect the payload. Empty batches remain a no-op.

- [ ] **Integrate both creation paths.** After saving, schedule the saved payloads instead of calling the push port directly. Keep single creation returning its existing `NotificationInfoDto`. Preserve current routing: single creation attempts both user and fanpage destinations when both are present; batch creation prefers the user destination and otherwise uses the fanpage destination. The two dispatcher entry points refine the parent plan's interface to preserve this existing distinction. Remove the misleading comment that assumes dispatch is asynchronous.

- [ ] **Isolate delivery failures.** Catch and log exceptions per destination inside the after-commit callback, continue remaining deliveries, and do not let a push failure turn a committed write into an apparent API failure. Log notification ID and destination identifiers without tokens or unnecessary payload data. Rollback must never trigger delivery.

## Verification

- [ ] **Cover dispatcher and service behavior.** Verify no push before commit; correct payload/routing after commit; no push on rollback; rejection outside a transaction; empty batches; and independent error handling for each destination. Verify snapshot contents do not change if the original model is mutated after scheduling. Update existing service tests to assert scheduling rather than immediate pushes.

- [ ] **Prove transaction behavior with PostgreSQL.** Use Spring-proxied services, real transaction boundaries, and a recording push adapter. Cover successful single/batch creation, insert failure, commit-time failure, and a failed batch leaving no saved rows or pushes. At push time, read the saved row through a separate connection to demonstrate it is already committed. Also verify that a push failure preserves rows and allows later deliveries, and that an outer rollback does not undo a successfully committed `REQUIRES_NEW` notification transaction. Do not rely solely on rollback-at-test-end fixtures or mocked repositories.

- [ ] **Run regressions and record results.** Run notification service/dispatcher tests, the PostgreSQL integration tests, and fanpage absence-alert tests. A missing database or skipped integration run is incomplete verification. Check the diff for unrelated changes and record test results with the delivery.

## Done when

Single and batch notifications become visible to push recipients only after their own successful commit. Failed writes produce neither partial batches nor pushes, and failed pushes leave committed data intact. Existing routing, API responses, and transaction propagation remain covered by tests.
