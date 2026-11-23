# Daily event recommendation refactor plan

**Goal.** A returning student sees an up-to-date list of roughly a dozen events without repeating history queries or Gemini scoring for unchanged events.

**Architecture.** Store event features once, freeze student progress and interests on first recommendation access each Vietnam day, and cache score components per student and event. At request time, check availability, apply the persistent view penalty and sort the small list in memory.

**Stack.** Java, Spring transactions, PostgreSQL and the existing Gemini port. Use this plan with the [daily recommendation design](../specs/2026-11-22-daily-event-recommendation-design.md). The scoring section below incorporates the later discussion and supersedes the spec’s binary criterion score, attendance-only history, independent urgency points and 30% Gemini weight.

**Status.** Proposed for review. Numerical defaults below are initial tuning choices. For execution, use the repository’s executing-plans or subagent-driven-development skill and track each checkbox.

## What triggers work?

| Trigger | Work performed |
|---|---|
| Event created or edited | Upsert its summary in the event transaction. Advance its scoring version only when a scoring input changes, including title or description sent to Gemini. Zero student rows touched. |
| Student’s first recommendation request of the local day | Build one snapshot, compute that student’s eligible event scores in batches, preserve every view penalty. |
| Another request that day | Reuse the snapshot. Score just the missing or stale rows for this student, including newly published events. Read availability and view state again. |
| Student earns points, completes criteria, follows a page or registers | Personalization picks up changes next day. Registration exclusion and available capacity take effect on the next list read. |
| Frontend submits pending viewed event IDs on return to the main page | Record one view per distinct event in the batch and deduct points up to the cap. The next recommendation request uses the new view state after the batch commits. |
| Deadline passes or a slot opens | Recompute the priority group and urgency during the list read. No Gemini call required. |

## Ranking contract

First exclude inactive or unpublished events, events whose happen **day** has passed in `Asia/Ho_Chi_Minh`, and events the student already holds a non-cancelled registration for. Proposed defaults from the design also exclude events before registration opens and events missing a required happen or close date. A missing opening timestamp allows immediate opening.

Order the eligible events in three strict groups: **open with slots and unseen → open with slots and viewed → closed or full**. A deadline equal to the request time is closed. Viewing an event persists across days. Within each group, sort by effective score descending, then earlier registration deadline, then event ID. Paginate last.

### Proposed starting values

| Component | Calculation and purpose |
|---|---|
| Useful category points, 0–50 | Simulate the award in the event’s semester. Apply category sum or maximum-child propagation, compare capped before/after root totals, and count each affected root once. Award 10 ranking points per useful point up to 50. |
| Criterion progress, 0–50 | Simulate the eligible active criterion in the event’s academic year. Give 0 for no new progress, 25 for completing the criterion, and 50 total if it also completes at least one previously incomplete parent through its required-child threshold. A previously completed parent adds zero bonus. Use the same threshold and ancestor propagation as actual awards. |
| Deadline multiplier, 1.00–1.10 | Multiply **only the sum of the two academic benefits**. For open events, ramp linearly from zero boost at 72 hours remaining to 10% immediately before close. Use elapsed seconds. Closed/full events receive zero boost. |
| Interest, 0–6 | Give 3 for a followed or active-member fanpage. Give 1 per distinct prior event registration of the same type, capped at 3. Include a standing registration before attendance and retain finalized positive history. Exclude cancelled and absent records. No timetable calculation. |
| Gemini, 0–4 | Validate a finite score in 0–100, then scale by 0.04. Use the frozen context and event features. Proposed fallback for invalid or missing output is the deterministic interest score scaled from 0–6 to 0–4. |
| Read penalty, 0–20 | Deduct 5 per event per acknowledged batch, preserving the deduction across daily rebuilds. The first view also moves an open event to the viewed group. |

Store the academic subtotal separately so urgency can amplify it live. Store interest and Gemini contributions with the daily score. Compute **effective score = daily score + academic subtotal × deadline boost − read penalty**. Compare full precision. For example, two unseen open events each offer two useful points and complete a new criterion. The one that also completes a parent starts at 70 academic points, the other at 45. With 36 hours left, those become 73.5 and 47.25 before interest and Gemini. An open unseen event with no academic gain can still surface through the interest signals. The 5-useful-point saturation and size of the parent bonus deserve review against actual event examples before rollout.

## Global constraints and review focus

Keep domain scoring pure and use an injected clock. Resolve identity in the application through SecurityContextPort. Keep refresh writes outside read-only query transactions. Preserve the current paged response envelope and existing benefit fields. The event-detail GET stays safe for anonymous access and prefetch. Backfill must preserve read history.

Review five cases closely: an award that cannot increase a maximum-child root; a child that crosses a parent’s threshold; a view racing a refresh; an event edit during Gemini evaluation; a full event that regains a slot on its happen day. Each appears in the checks below.

## Implementation sequence

Paths below are relative to this worktree. Java package paths use the existing module roots: `D` = `ddd-domain/src/main/java/com/ddd/domain`, `A` = `ddd-application/src/main/java/com/ddd/application`, `I` = `ddd-infrastructure/src/main/java/com/ddd/infrastructure`, and `API` = `ddd-api/src/main/java/com/ddd/api`. Test files use the corresponding module’s `src/test/java` tree with the same package.

### 1. Make academic benefit and ordering independently testable

- [ ] Add failing cases to a new `D/model/recommendation/RecommendationScoringPolicyTest` in the test tree. Cover a +5 leaf award below a sibling maximum, two leaves sharing a root, wrong award year, an inactive criterion, a parent one child short, and an already-completed parent. Assert the concrete 70/45 example above.
- [ ] Create `D/model/recommendation/RecommendationScoringPolicy.java` with `benefits(StudentSnapshot, EventSummary)` returning named component values. Define the two snapshot records and `RecommendationBenefits` in that package. Reuse award propagation semantics, including cascades, without mutating student records.
- [ ] Add ordering tests with an extreme valid AI result, midnight, exact close and re-opened capacity. Implement the group and effective-score comparison independently of database access. Run the domain tests and commit once they pass.

### 2. Persist and backfill event features

- [ ] Add a PostgreSQL integration test in `I/repository/RecommendationStoreIntegrationTest` covering existing events, rerunning backfill, and an edit rolling back with its source event.
- [ ] Add `ddd-infrastructure/src/main/resources/database/migration/daily-event-recommendation.sql`. Create event and student summary tables keyed by their source IDs, student_event_recommendation keyed by student/event, and view receipts unique on student/event/batch UUID. Permit unset score freshness for an already-viewed row. Use foreign keys and indexes supporting candidate reads and the per-student lookup.
- [ ] Add `D/repository/RecommendationStore.java` and its JDBC adapter `I/repository/RecommendationStoreImpl.java`. Wire event-summary upsert into `I/repository/EventRepositoryImpl.java` after point rows are saved. Backfill and normal writes must produce equivalent features. Audit publication, archive and bulk-edit paths for writes bypassing save; source live status and capacity from authoritative tables.
- [ ] Verify changed scoring features advance the version, a date-only edit updates displayed dates without a score rebuild, and creating an event writes zero student rows. Run the PostgreSQL tests and commit.

### 3. Build the frozen snapshot and refresh stale scores

- [ ] Extend `A/service/event/impl/EventRecommendationServiceImplTest` with a fake clock and counting repositories. Two requests on one day should produce one snapshot load. Registering or earning points between requests must leave that snapshot intact. A new or edited event must refresh only its requesting student’s affected row.
- [ ] Add `I/repository/StudentRecommendationSnapshotLoader.java` behind a domain repository contract of the same name in `D/repository` (use an Impl suffix on the adapter). Batch-load points and category trees by semester, eligible criteria and completed children by academic year, followed/member pages and counts by registered event type. Include the periods needed to score events added later that day.
- [ ] Add `A/service/event/impl/RecommendationRefreshService.java` with transactional `prepare(long studentId, Instant now)`. Conflict-safely initialize and lock the student summary row; refresh on local date change. Compare score date, snapshot version and captured event version. Upsert score columns only. A simultaneous event edit must leave the old evaluated version visible for repair on the next request.
- [ ] Adapt `D/model/port/EventRecommendationAiPort.java`, its existing context/candidate records and `I/adapter/ai/GeminiEventRecommendationAdapter.java`. Batch up to 10 stale events at a time. Ignore unknown IDs, use per-event fallback for missing/invalid results, and persist explanations. Bound calls with timeouts while the student lock is held.
- [ ] Test 11 stale candidates, partial invalid output, timeout fallback, simultaneous first requests and rollover at Vietnam midnight. Assert no second call on a warm request. Run the application, adapter and PostgreSQL checks and commit.

### 4. Record view batches atomically

Use a client-generated batch UUID to deduplicate transport retries while retaining the capped deduction for an event viewed again in a later batch. The backend stores a receipt per accepted event and batch without persisting a browsing session.

- [ ] Add PostgreSQL cases for duplicate IDs in one list, same-batch retries, the same event in two batches, a batch before first scoring and a batch racing daily refresh. Five distinct batches reporting the same event must yield a penalty of 20.
- [ ] Implement `recordViews(long studentId, UUID batchId, Set<Long> eventIds, Instant now)`. Insert one receipt per student, event and batch, then apply the capped penalty only to newly inserted receipts in the same transaction.
- [ ] Add authenticated `POST /api/events/views` to EventController. Accept `batchId` and `eventIds`. Resolve identity server-side and check visibility in bulk. Skip IDs no longer visible so one stale event does not block the rest. An empty list succeeds without a write.
- [ ] Have the frontend collect IDs in a per-student pending set shared across routes after detail pages load. On main-page activation, including browser Back and site navigation, capture the pending IDs into a batch and POST them before fetching recommendations. Serialize submissions. On success, clear only the acknowledged batch and fetch the list. Preserve any IDs collected while the batch was in flight. On failure, retain the exact batch and its UUID for retry and show the existing list. If there is no pending batch or ID, use normal list fetching or caching. Verify the flow with Main → Event 42 → Profile → Event 57 → Main and expect one POST containing both IDs before the recommendation GET.

### 5. Switch the recommendation read path and verify rollout

- [ ] Extend the service and API tests to check eligible totals, deterministic ties across pages, a full event regaining a slot, and a past timestamp earlier today surviving until local midnight.
- [ ] Update `A/service/event/EventRecommendationService.java` and `A/service/event/impl/EventRecommendationServiceImpl.java` to orchestrate identity, preparation and live read. Check the legacy entry point in `A/service/event/impl/EventQueryServiceImpl.java` for read-only transaction inheritance. Bypass the 30-minute result cache. Batch live status, registration exclusion, slot counts and score/penalty rows; hydrate only the selected page, preserving sorted order.
- [ ] Update `API/controller/EventController.java`, `A/dto/recommendation/EventRecommendationDto.java`, `API/dto/event/res/EventRecommendationResponseDto.java` and `API/mapper/EventApiMapper.java`. Expose availability reason, viewed flag, personalized score, urgency contribution, penalty and effective score. Verify existing event fields and aiExplanation through the HTTP contract.
- [ ] Run `mvn -o -B -pl ddd-api -am test` and the PostgreSQL-backed checks against an isolated test database. Apply the additive migration through the project SQL process before switching reads. Compare source and summary IDs and canonical features for existing candidates. Confirm a repeat backfill preserves penalties.
- [ ] Measure first daily access, repeated access and a one-event edit with roughly 12 candidates. Record query count, Gemini calls, refresh lock wait and latency for each. Warm acceptance requires zero history aggregations, zero Gemini calls, and batched reads whose count does not grow per event. Sorting costs O(E log E); the expected saving comes from avoiding repeated aggregation and external calls. Commit the read-path change after these checks.

**Rollout check.** Review sample rankings with actual category awards and parent thresholds. Keep the existing read implementation available for rollback during verification; preserve the new tables and receipts if reads are rolled back.


## Deployment and implementation notes

Apply `event-view-batch-migration.sql` followed by `daily-event-recommendation.sql` from the infrastructure migration directory before deploying the new recommendation endpoint. Apply them using the project’s SQL deployment process. The second migration backfills event features and preserves existing view rows.

Event summary synchronization is implemented by PostgreSQL triggers on events, event points and related scoring fields. This covers direct and bulk writes as well as repository saves. Availability dates are read live. The daily snapshot version is its Vietnam date. A warm request checks for refresh work before locking or loading the snapshot. The existing recommendation service stays available as a rollback path.

The frontend integration is a separate deliverable. Send the view batch before the recommendation GET as described in Task 4. Recommendation responses expose `registrationClosed`, `full`, `viewed`, `personalizedScore`, `urgencyScore`, `readPenalty` and `effectiveScore`. Treat `matchScore` as an effective ranking score.
