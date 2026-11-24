# Recommendation refactor • production code preview

Read alongside the [implementation plan](2026-11-23-event-recommendation-refactor.md). This is proposed code for review before editing the service. Listings show changed methods, record shapes and significant SQL. Imports, constructor injection, serialization plumbing and unchanged methods are omitted. New names are proposals. The snippets have not been compiled.

## 1. Domain inputs and stored results

In `ddd-domain/.../model/recommendation`, use immutable value records. Keep the score and the view fields independently writable. Period maps are keyed by the **event’s** award period.

```java
record EventSummary(long id, long version, String title, String description,
                    Long semesterId, Long academicYearId, Long typeId, Long fanpageId,
                    Long criterionId, Map<Long,Integer> offeredPoints) {}

record Category(long id, Long parentId, boolean maxChild, int maximum, boolean active) {}
record Criterion(long id, Long parentId, int requiredChildren, boolean active,
                 boolean eligibleForEvent) {}
record PeriodPoints(Map<Long,Integer> totals, Map<Long,Category> categories) {}
record YearCriteria(Set<Long> completed, Map<Long,Criterion> criteria) {}
record StudentSnapshot(long version, LocalDate date, Map<Long,PeriodPoints> semesters,
                       Map<Long,YearCriteria> years, Set<Long> fanpages,
                       Map<Long,Integer> registrationsByType) {}
record Benefits(int usefulPoints, boolean newCriterion, boolean completesParent,
                boolean fanpageMatch, int typeMatches) {
    double academic() {
        int criterion = completesParent ? 50 : newCriterion ? 25 : 0;
        return Math.min(50.0, 10.0 * usefulPoints) + criterion;
    }
    int interest() {
        return (fanpageMatch ? 3 : 0) + Math.min(3, typeMatches);
    }
}
record DailyScore(Benefits benefits, double aiContribution, String explanation) {
    double personalized() {
        return benefits.academic() + benefits.interest() + aiContribution;
    }
}
```

The persisted score also carries score date, evaluated event version, student snapshot version and per-category gain details for the existing response. Use empty period objects on missing IDs and zero benefit for unavailable award periods. Store historical registrations as distinct events, so a registration progressing through multiple statuses contributes once.

## 2. RecommendationScoringPolicy

This class uses the daily snapshot. No repository or Gemini call belongs in these methods.

```java
Benefits benefits(StudentSnapshot s, EventSummary e) {
    var points = periodOrEmpty(s.semesters(), e.semesterId());
    var year = yearOrEmpty(s.years(), e.academicYearId());
    var c = e.criterionId() == null ? null : year.criteria().get(e.criterionId());
    boolean advances = c != null && c.active() && c.eligibleForEvent()
            && !year.completed().contains(c.id());
    boolean parent = advances && completesParent(year, c);
    boolean follows = e.fanpageId() != null && s.fanpages().contains(e.fanpageId());
    int matches = e.typeId() == null ? 0 : s.registrationsByType().getOrDefault(e.typeId(), 0);
    return new Benefits(marginalRootGain(points, e.offeredPoints()), advances, parent,
                        follows, matches);
}

boolean completesParent(YearCriteria year, Criterion child) {
    if (child.parentId() == null) return false;
    var parent = year.criteria().get(child.parentId());
    if (parent == null || !parent.active() || year.completed().contains(parent.id())) return false;
    long alreadyComplete = year.criteria().values().stream()
            .filter(c -> Objects.equals(c.parentId(), parent.id()))
            .filter(c -> year.completed().contains(c.id())).count();
    return alreadyComplete < parent.requiredChildren()
            && alreadyComplete + 1 >= parent.requiredChildren();
}
```

Precompute the completed-child counts once per snapshot if the award tree is large. Normalize a missing required-child threshold to the existing default of one. The proposed bonus is paid once even when an ancestor chain completes.

```java
int marginalRootGain(PeriodPoints period, Map<Long,Integer> offered) {
    var simulated = new HashMap<>(period.totals());
    var affectedRoots = new HashSet<Long>();
    for (var award : offered.entrySet()) {
        var node = period.categories().get(award.getKey());
        if (node == null || !node.active() || award.getValue() <= 0) continue;
        // Extract/reuse the same pure propagation used when awarding points.
        // It updates simulated totals up the sum/max-child chain and returns its root.
        affectedRoots.add(applyAward(simulated, period.categories(), node, award.getValue()));
    }
    int gain = 0;
    for (long id : affectedRoots) {
        int max = period.categories().get(id).maximum();
        int before = Math.min(max, period.totals().getOrDefault(id, 0));
        int after = Math.min(max, simulated.getOrDefault(id, 0));
        gain += Math.max(0, after - before);
    }
    return gain;
}
```

### Live ordering in the same policy

```java
Rank rank(LiveCandidate e, Instant now) {
    boolean closed = !now.isBefore(e.close());
    boolean full = e.remainingSlots() == 0;
    boolean available = !closed && !full;
    int group = available ? (e.lastReadAt() == null ? 0 : 1) : 2;
    double secondsLeft = Duration.between(now, e.close()).toMillis() / 1000.0;
    double boost = available ? 0.10 * Math.clamp(1 - secondsLeft / 259200.0, 0, 1) : 0;
    double urgency = e.score().benefits().academic() * boost;
    double effective = e.score().personalized() + urgency - e.readPenalty();
    return new Rank(e.id(), group, e.close(), urgency, effective, closed, full);
}

static final Comparator<Rank> ORDER = Comparator.comparingInt(Rank::group)
        .thenComparing(Comparator.comparingDouble(Rank::effective).reversed())
        .thenComparing(Rank::close).thenComparingLong(Rank::id);
```

All scoring constants here illustrate the proposed starting values. Bind the approved values once in validated configuration. No calendar or time-overlap input enters the calculation.

## 3. EventRepositoryImpl and RecommendationStoreImpl

In `ddd-infrastructure/.../repository/EventRepositoryImpl.save`, retain the existing entity preparation, association resolution and point synchronization. Replace the final save/map with the following inside the event transaction.

```java
EventJpaEntity saved = eventJpaRepository.saveAndFlush(entity);
Event result = eventMapper.toDomainModel(saved);
recommendationStore.syncEvent(saved.getId());
return result;
```

In `RecommendationStoreImpl.syncEvent(long id)`, build a canonical JSONB object from the event, award period and grouped point rows. Include prompt text and every deterministic scoring input. Keep timestamps used solely for live ranking outside that JSON. Backfill uses the same projection as normal saves. PostgreSQL compares JSONB structurally.

```sql
INSERT INTO event_recommendation_summary AS s (event_id, features, scoring_version)
VALUES (:eventId, CAST(:features AS jsonb), 1)
ON CONFLICT (event_id) DO UPDATE SET
    features = EXCLUDED.features,
    scoring_version = s.scoring_version + 1
WHERE s.features IS DISTINCT FROM EXCLUDED.features
```

`RecommendationStore` is the domain port, with JDBC implementing the following operations. Lock only the recommendation summary for the requesting student.

```java
interface RecommendationStore {
    void syncEvent(long eventId);
    SnapshotRow lockOrCreate(long studentId);
    void saveSnapshot(long studentId, StudentSnapshot snapshot);
    List<EventSummary> staleEligible(long studentId, StudentSnapshot snapshot, Instant now);
    void saveScore(long studentId, StudentSnapshot snapshot, EventSummary event, DailyScore score);
    List<LiveCandidate> listEligible(long studentId, Instant now);
    void recordViews(long studentId, UUID batchId, Set<Long> eventIds, Instant now);
}
```

`lockOrCreate` inserts the student key with ON CONFLICT DO NOTHING, then reads it FOR UPDATE. `staleEligible` left joins student scores by student/event and selects missing rows or distinct snapshot/event versions. `saveScore` upserts only score columns, including the version from the event object supplied to Gemini. Both reads share the live eligibility predicate: published, active, happen timestamp at or after Vietnam start-of-day, opening timestamp null or reached, closing timestamp present, and no non-cancelled registration by this student. Keep past deadlines and full events in the result. Group active registration counts once by event. Clamp remaining slots to zero.

## 4. StudentRecommendationSnapshotLoaderImpl and RecommendationRefreshService

The JDBC loader implements `StudentSnapshot load(long studentId, LocalDate day, long version)`. Assemble the snapshot from bulk queries for semester category totals, category structure, academic-year criterion completions and thresholds, followed/member fanpages and type history. Use distinct registered event IDs grouped by type with status other than CANCELLED or ABSENT, including REGISTERED and RECORDED. Only a new daily snapshot runs these queries.

```java
@Transactional
public void prepare(long studentId, Instant now) {
    LocalDate today = now.atZone(VIETNAM).toLocalDate();
    SnapshotRow row = store.lockOrCreate(studentId);
    StudentSnapshot snapshot = row.snapshot();
    // A late-arriving request must never overwrite tomorrow's snapshot with yesterday's.
    if (snapshot == null || snapshot.date().isBefore(today)) {
        snapshot = loader.load(studentId, today, row.version() + 1);
        store.saveSnapshot(studentId, snapshot);
    }
    List<EventSummary> stale = store.staleEligible(studentId, snapshot, now);
    for (int start = 0; start < stale.size(); start += 10) {
        var batch = stale.subList(start, Math.min(start + 10, stale.size()));
        scoreBatch(studentId, snapshot, batch);
    }
}
```

`scoreBatch` computes Benefits for each ID, builds one candidate list, and calls the existing AI port once. Match returned IDs against that batch. For each candidate persist the following contribution and explanation.

```java
boolean valid = reply != null && Double.isFinite(reply.aiScore())
        && reply.aiScore() >= 0 && reply.aiScore() <= 100;
double ai = valid ? reply.aiScore() * 0.04 : benefit.interest() * (4.0 / 6.0);
var score = new DailyScore(benefit, ai,
        valid ? reply.aiExplanation() : explainBenefits(benefit));
store.saveScore(studentId, snapshot, event, score);
```

## 5. EventRecommendationAiPort and GeminiEventRecommendationAdapter

Keep the existing port method `generateExplanations(StudentRecommendationContext, List<CandidateEventRecommendation>)` and `AiRecommendationResult` record. Change the two prompt input records to carry period-aware benefit evidence and bounded event text. Build descriptions with a configured character limit. Supply category and criterion names, the parent-completion flag and interest signals; omit email, remaining slots, deadline urgency and views. Parse each item independently. On timeout or unavailable credentials return an empty result list; a missing or invalid item falls back individually in scoreBatch. Remove the adapter’s synthetic AI fallback so it cannot be mistaken for a model response.

## 6. RecommendationViewService and batch receipts

```java
@Transactional
public void recordBatch(UUID batchId, List<Long> eventIds) {
    long studentId = requireCurrentUserId();
    var visibleIds = visibility.visibleEventIds(studentId, Set.copyOf(eventIds));
    if (!visibleIds.isEmpty()) {
        store.recordViews(studentId, batchId, visibleIds, clock.instant());
    }
}
```

Bind the checked IDs as a PostgreSQL bigint array. The receipt has a unique key on student, event and batch. A conflict yields no row for the penalty update.

```sql
WITH accepted AS (
    INSERT INTO recommendation_view_receipt (student_id, event_id, batch_id)
    SELECT :studentId, event_id, :batchId
    FROM unnest(CAST(:eventIds AS bigint[])) AS batch(event_id)
    ORDER BY event_id
    ON CONFLICT DO NOTHING RETURNING student_id, event_id
)
INSERT INTO student_event_recommendation AS r
    (student_id, event_id, read_penalty, last_read_at)
SELECT student_id, event_id, 5, :now FROM accepted ORDER BY event_id
ON CONFLICT (student_id, event_id) DO UPDATE SET
    read_penalty = LEAST(20, r.read_penalty + 5),
    last_read_at = GREATEST(r.last_read_at, EXCLUDED.last_read_at)
```

## 7. EventRecommendationService, EventRecommendationServiceImpl and EventQueryServiceImpl

The recommendation service exposes `Page<EventRecommendationDto> getRecommendations(Pageable pageable)`. The facade itself has no read-only transaction. Inject the transactional refresh service as a separate Spring bean.

```java
public Page<EventRecommendationDto> getRecommendations(Pageable pageable) {
    Long studentId = security.getCurrentUserId();
    if (studentId == null) throw new UnauthorizedException("User is not authenticated");
    Instant now = clock.instant();
    refresh.prepare(studentId, now);
    var ranked = store.listEligible(studentId, now).stream()
            .map(candidate -> rank(candidate, now)).sorted(ORDER).toList();
    int from = (int) Math.min(pageable.getOffset(), ranked.size());
    int to = (int) Math.min((long) from + pageable.getPageSize(), ranked.size());
    var page = hydrateInOrder(studentId, ranked.subList(from, to));
    return new PageImpl<>(page, pageable, ranked.size());
}
```

The read projection carries the stored benefits and live flags needed by hydrateInOrder. Batch-fetch page details, index by ID and map in rank order. If publication races preparation, a newly visible row lacking a daily score is picked up on the next request. In the legacy EventQueryServiceImpl shortcut use method-level transaction propagation NOT_SUPPORTED and delegate to the facade after enforcing the caller’s identity. Bypass RecommendationCacheService on this path.

## 8. EventController, EventApiMapper and response records

```java
@GetMapping("/recommendations")
public BaseResponse<Page<EventRecommendationResponseDto>> getRecommendations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    return BaseResponse.of(eventRecommendationService
            .getRecommendations(PageRequest.of(page, size))
            .map(eventApiMapper::toRecommendationResponseDto));
}

public record ViewedEventsRequest(@NotNull UUID batchId,
                           @NotNull List<@NotNull @Positive Long> eventIds) {}

@PostMapping("/views")
public BaseResponse<Void> recordViews(@Valid @RequestBody ViewedEventsRequest request) {
    recommendationViewService.recordBatch(request.batchId(), request.eventIds());
    return BaseResponse.of((Void) null);
}
```

Append these components to both EventRecommendationDto and EventRecommendationResponseDto, preserving their current event and explanation mappings. Use same-name MapStruct mapping in EventApiMapper.

```java
boolean registrationClosed, boolean full, boolean viewed,
double personalizedScore, double urgencyScore, int readPenalty, double effectiveScore
```

Populate legacy matchScore from the effective ranking score and document its units for consumers. The frontend sends a batch UUID and distinct pending event IDs when the main page becomes active, then fetches recommendations after acknowledgment. Retries reuse the same batch UUID and payload. The stored timestamp reflects when the backend accepted the report. Fetching or preloading an event detail has no effect on read history.
