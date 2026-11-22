# Daily event recommendation design

**Status:** Awaiting written specification approval before implementation planning.
**Date:** 22 November 2026
**Worktree:** `ddd-recommendation`, branch `fea/recomendation`, reviewed at `fe410dd`.

## Purpose and agreed behavior

Provide useful event discovery with approximately a dozen concurrent events while avoiding repeated student-history aggregation and Gemini calls on every recommendation request. Favor unseen events that still accept registrations. Keep the algorithm readable through focused components and explicit ordering rules.

The agreed requirements are:

1. Persist event features when an event is created or relevant details change.
2. Refresh a student's personalization snapshot on their first recommendation access each Vietnam calendar day. Later point awards, criteria completion, and affiliation changes take effect the next day.
3. Persist personalized scores per student and event. Recalculate them on a new day, when absent, or when the event's scoring features have changed.
4. Event changes update the event summary once. They do not trigger recalculation for every student. Each returning student updates only their missing or outdated scores.
5. Every distinct event view increases a persistent penalty, up to a cap. Daily recalculation preserves penalties and view history.
6. Include active, published events whose happen date is today or later in Vietnam time. A happen timestamp earlier today does not remove the event until that calendar day ends.
7. Use strict priority groups: available and unseen; available and viewed; then unavailable. Unavailable means registration has closed or capacity is full.
8. Within a priority group, order by daily personalized score plus current registration urgency minus persistent read penalty.
9. Keep Gemini in personalized scoring. Accept slower first-access refreshes in exchange for AI influence; validate its output and retain fallback behavior.

## Proposed defaults for review

The following fill implementation details that have not yet been explicitly selected. Approving this specification would approve these defaults; they are not claimed as earlier user decisions.

| Setting or boundary | Proposed behavior |
|---|---|
| Time zone | `Asia/Ho_Chi_Minh`; use an injected `Clock` for every time-dependent calculation |
| View deduction | 5 score points per distinct view request |
| Penalty cap | 20 score points, preserved across days |
| Registration urgency | Maximum 10 points, increasing linearly over the last 72 hours before `dateClose` |
| Gemini batch size | At most 10 outdated or missing event scores per call; evaluate all such events in batches rather than privileging only the first ten |
| Previously registered events | Preserve existing behavior: exclude an event if the student's registration status is anything other than `CANCELLED` |
| Not yet open for registration | Exclude until `dateOpen`; do not confuse these events with already-closed events |
| Missing dates | Exclude malformed events with missing registration close or happen timestamps; log the data issue |
| Tie ordering | Earlier registration deadline, then ascending event ID |
| Refresh failure | Use deterministic scores when Gemini fails; publish a new daily snapshot and its corresponding scores atomically |

The read deduction, penalty cap, urgency window, and Gemini timeout/batch settings belong in one validated recommendation configuration. Scores are ranking values rather than probabilities. Effective scores may be negative or above 100; the response must not present them as percentages.

## Summary storage

Use ordinary PostgreSQL tables and the existing transaction infrastructure. No scheduler, message broker, vector database, candidate heap, or update of all student-event combinations is required.

### Event recommendation summary

One row per event stores the event's type, fanpage, criterion, semester and academic year, offered category points, dates, relevant display names, and a monotonically increasing `scoringVersion`. Store compact structured point data rather than full JPA entity graphs.

Keep a feature summary and its source event consistent in the same database transaction. Integrate summary persistence at the event repository save boundary so all application paths saving an event are covered, rather than copying refresh logic into individual controllers. An event update compares canonical scoring features: changing points, type, fanpage, criterion, or award period advances `scoringVersion`; changing a banner or description alone does not change deterministic features, but description changes must advance the version while Gemini uses descriptions for scoring. Dates update the summary, with urgency and availability derived at read time.

Creation and edits never write student recommendation rows. Backfill existing events before switching reads to summaries. Provide a repair/backfill operation for missing summaries so deployments do not silently omit existing events.

Live event status and activity remain authoritative. Capacity and active-registration counts are read in one batch at request time. Passage of time never depends on a scheduled update.

### Student recommendation summary

One row per student contains `snapshotDate`, `snapshotVersion`, and a structured snapshot of points by semester and category, completed criteria by academic year, followed/member fanpages, and attendance affinity by event type and point category. Include the category tree and eligibility metadata needed to interpret these records consistently for that day.

Snapshot progress by award period, rather than storing one undifferentiated current-semester deficit. Event awards belong to the event's semester and academic year. Preserve a complete enough period snapshot to score new events encountered later that day without refreshing live student progress. A missing student progress row means zero progress within a valid period; an event with no valid award period receives no claimed point or criteria benefit.

Include `CHECKED_IN`, `COMPLETED`, `CONFIRMED`, and `RECORDED` when building attendance affinity, so event finalization does not erase successful history. Exclude `CANCELLED`, `ABSENT`, and unconfirmed self-attendance states.

`snapshotDate` is the Vietnam date captured at refresh start. Once refresh completes, all requests on that date use the same snapshot. A request after midnight may need the next day's snapshot even if the previous refresh completed after midnight. Student changes do not invalidate this snapshot during the day.

### Student event recommendation

One durable row per `(studentId, eventId)` contains:

- `scoreDate`, `studentSnapshotVersion`, and `evaluatedEventVersion`.
- The deterministic component scores, Gemini score, final personalized score, explanation, and calculated benefit details.
- `readPenalty`, `lastReadAt`, and audit metadata.

Daily or event-version score updates modify only score-related fields. View updates modify only penalty and view fields. Use column-specific updates so a refresh cannot overwrite a concurrent view penalty.

Do not store a position number or registration urgency: both depend on current request context. An existing row with `lastReadAt` set remains viewed after its daily score changes.

### View request receipts

Every user navigation into event details gets a new client-generated `viewRequestId`. Retrying that navigation reuses its ID. Store a receipt with a unique `(studentId, eventId, viewRequestId)` key in a small supporting table. Insert the receipt and update the penalty atomically. Duplicate IDs are successful no-ops; distinct IDs each apply a deduction until the cap.

This supporting storage distinguishes genuine repeated views from network retries. Do not implement the penalty as a side effect of the existing public event-detail GET: prefetches, anonymous requests, and retries would otherwise create unintended penalties. Receipt cleanup is outside the initial scope; storage growth must be monitored and a future retention policy must preserve the documented retry contract.

## Personalized scoring and live ranking

### Deterministic score

Retain the existing relative personalization weights, separating urgency into the live ranking stage:

```text
weightedPersonalization = 0.35 × pointScore
                        + 0.25 × criteriaScore
                        + 0.15 × fanpageScore
                        + 0.15 × historyScore

heuristicScore = weightedPersonalization / 0.90
```

Each component is bounded between 0 and 100, so normalization produces a heuristic score between 0 and 100.

For points, simulate the student's before/after category totals using the same sum and maximum-child rules as actual award propagation. Sum positive changes once at category roots, capped by each root's remaining maximum for recommendation benefit. Do not sum both leaves and their ancestors. The point score is `min(100, marginalRootGain × 20)`; response details distinguish raw points offered from marginal aggregated benefit. Events with invalid/inactive point references receive no benefit for those references.

The criteria score is 100 only for an active criterion the student has not completed in the event's academic year and that is eligible for event completion. Otherwise it is zero. Fanpage affinity is 100 for either followed or active-member fanpages. History keeps the existing event-type contributions of 40 for one attendance and 60 for two or more, plus 40 for any matching attended category, capped at 100.

### Gemini contribution

Gemini receives the frozen student context and only the event rows needing score recalculation. Supply readable category/criterion names and explicit award periods. Bound descriptions and batches. Daily scoring does not use live urgency or read penalties in the prompt: those are separate ranking inputs and must not be counted twice.

```text
personalizedScore = 0.70 × heuristicScore + 0.30 × validatedAiScore
```

Reject non-finite or out-of-range scores and unknown event IDs. For a failed call, malformed response, missing event, or invalid score, use that event's heuristic score and deterministic explanation. Preserve available valid results. Persist fallback scores for the day rather than retrying Gemini on every request. A later event-version change or next-day refresh may invoke Gemini again.

All candidates follow the same formula. Remove the old rule that caps every event outside the first ten below the AI-evaluated group. Batching controls payload size rather than determining a privileged ranking group.

### Live ordering

On every request, capture one `now` and derive `today` from it. Exclude inactive/unpublished events, events with a happen date before `today`, registrations already held by this student, and not-yet-open events.

An event is available when `dateOpen <= now < dateClose` and `remainingSlots > 0`. At exactly `dateClose` it is closed. Use `max(0, capacity - activeRegistrations)` for displayed slots. Closed/full events remain in the third group until their happen day passes.

```text
priorityGroup = 0 for available and never viewed
                1 for available and viewed
                2 for closed or full

urgency = available
        ? 10 × clamp(1 - hoursUntilRegistrationClose / 72, 0, 1)
        : 0

effectiveScore = personalizedScore + urgency - readPenalty
```

Sort by group ascending, effective score descending, registration deadline ascending, event ID ascending. Keep internal precision through comparison; round only response fields. Pagination happens after filtering and sorting, and totals describe the currently eligible list. Full events that regain capacity immediately return to group 0 or 1 without rescoring.

## Request and update flows

### Recommendation request

1. Resolve identity through `SecurityContextPort`; reject unauthenticated requests using the existing domain exception.
2. In a command phase, initialize missing summary storage and serialize refreshes for this student. If the daily snapshot is stale, build it and score all eligible events. Otherwise, score only rows absent or with a mismatched event version.
3. Commit snapshot and score updates without changing penalty/view fields. The simplest initial implementation may hold a lock on the student's summary row during the bounded Gemini refresh; only that student's recommendation refresh waits. This consumes a connection during the external call, so retain bounded timeouts and measure waiting time. Do not lock the general user row or all event rows.
4. In a separate read phase, load persisted scores and current availability, calculate urgency, apply groups, and paginate.
5. Hydrate display DTOs for the selected page, preserving its established ordering and providing current registered counts.

Use a unique student summary key plus conflict-safe initialization before acquiring the row lock. A process-local mutex is insufficient if multiple application instances serve the student. The new command phase must run before entering a read-only query transaction; avoid self-invocation that bypasses Spring's transaction proxy.

### Event change

Save the event and its summary together. Advance only the affected event's scoring version when its scoring inputs change. Student rows remain untouched. A subsequent student request compares versions and refreshes only outdated scores using that student's existing daily snapshot.

Capture the evaluated event version with its scoring inputs. If an event changes while Gemini is evaluating it, the saved score must retain that old evaluated version; never label it as evaluated against the newer version. The next request detects and repairs the mismatch. Current eligibility still comes from current event state.

### Event view

The authenticated frontend posts `eventId` and `viewRequestId` after the user opens event details. Resolve the student from the security port. Validate event visibility, insert the receipt, and atomically set `readPenalty = min(readPenalty + 5, 20)` and `lastReadAt` for a new receipt.

A view can arrive before any recommendation request. Create a student-event row containing view state without forcing a daily snapshot or Gemini call; leave score freshness unset so the next recommendation request computes it. Being at the cap still records the event as viewed. No global ranking recomputation is necessary.

## Components and repository boundaries

Separate responsibilities into:

- A domain scoring policy for deterministic benefit calculations, score blending, urgency, and ordering.
- An event-summary repository adapter that synchronizes canonical event features on event saves.
- A student snapshot loader for the once-per-day source aggregations.
- A recommendation command service for preparation, refresh coordination, and view updates.
- A read-only recommendation query service for current availability, grouping, sorting, and response composition.
- The existing AI port and adapter for bounded Gemini evaluation with validated fallback.

Keep domain models and contracts independent of infrastructure classes. Application services depend on domain ports and repository contracts. Infrastructure owns JDBC/JPA access and serialization of structured summaries. Reuse `BaseModel`, `BaseEntityJpa`, `SecurityContextPort`, domain exceptions, `BaseResponse`, `ServiceUtil`, and the existing MapStruct mapper layers.

The old 30-minute full-result cache must not serve this flow: it would freeze priority groups, deadlines, and view penalties. Leave unrelated classes intact until their removal is explicitly authorized. The legacy query-service shortcut must not invoke a write-capable preparation phase inside a read-only transaction.

The response should expose availability, viewed state, personalized score, urgency score, read penalty, and effective score while retaining existing event and benefit fields. The current `aiExplanation` field can remain as a compatibility name even when deterministic fallback produced its content. Full/closed events need an explicit availability reason so their lower placement is understandable.

## Complexity and performance expectations

For `E` candidate events, a warm request reads compact summaries and live availability, then sorts in `O(E log E)`. It performs no student-history aggregation and no Gemini call when score dates and event versions match. Rendering costs scale with page size and associated page details. A daily refresh also pays source aggregation, benefit calculation, and Gemini evaluation for the small eligible set. An edit causes per-returning-student work only for changed events.

This design reduces repeated work; it does not eliminate repository queries or guarantee a latency target without measurement. Use a fixed number of batched availability queries, avoid JPA queries per event, and instrument daily refresh count, Gemini calls, refresh waiting, warm/cold latency, and candidate count. No heap or approximate shortlist is needed at the stated volume.

## Verification and delivery boundaries

Tests must cover strict group precedence even under extreme AI scores; deadline equality; events earlier today versus yesterday; full-to-available transitions; Vietnam midnight; unchanged same-day requests making no AI/history calls; new and edited events affecting only the requesting student; penalty preservation across refreshes; distinct versus retried views; concurrent refreshes/views; invalid and partially valid AI output; award-period isolation; inactive criteria; maximum-child marginal benefit; and pagination totals after live filtering.

Use database-backed PostgreSQL tests for unique constraints, row locking, atomic capped updates, and backfill idempotence. Mocks alone cannot verify those guarantees. Existing source-level repository tests and the current Maven module build remain part of regression verification. The frontend must send distinct view IDs for distinct navigations and reuse IDs for retries; implementing only the backend endpoint does not complete view tracking in a separate frontend.

Create a migration using the repository's existing SQL migration convention, including summary tables, uniqueness and lookup indexes, and existing-event backfill. Keep it additive and document how it is applied; do not assume the project already runs Flyway. Rebuild summary data from source records if needed, but never discard persistent read penalties during routine refresh or backfill.

No application implementation, migration execution, deployment, push, or deletion is authorized by this specification alone. After review, write a task-by-task implementation plan covering exact files, signatures, DTO mappings, migrations, tests, and rollout checks.
