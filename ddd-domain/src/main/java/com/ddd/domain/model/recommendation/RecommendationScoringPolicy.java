package com.ddd.domain.model.recommendation;

import com.ddd.domain.model.recommendation.RecommendationFeatures.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RecommendationScoringPolicy {
    public static final Comparator<Ranked> ORDER = Comparator.comparingInt(Ranked::group)
            .thenComparing(Comparator.comparingDouble(Ranked::effectiveScore).reversed())
            .thenComparing(rank -> rank.candidate().deadline())
            .thenComparingLong(Ranked::id);

    public Benefits benefits(Snapshot snapshot, EventFeatures event) {
        Long yearId = event.yearId();
        Set<Long> completed = yearId != null && snapshot.validYears().contains(yearId)
                ? snapshot.completedByYear().getOrDefault(yearId, Set.of()) : Set.of();
        Criterion criterion = event.criterionId() == null ? null : snapshot.criteria().get(event.criterionId());
        boolean advances = yearId != null && snapshot.validYears().contains(yearId)
                && criterion != null && criterion.active() && !completed.contains(event.criterionId());
        boolean parentBonus = advances && completesParent(snapshot.criteria(), completed, criterion);
        boolean fanpageMatch = event.fanpageId() != null && snapshot.fanpages().contains(event.fanpageId());
        int typeMatches = event.typeId() == null ? 0 : snapshot.typeCounts().getOrDefault(event.typeId(), 0);

        Long semesterId = event.semesterId();
        Map<Long, Integer> before = semesterId != null && snapshot.validSemesters().contains(semesterId)
                ? snapshot.pointsBySemester().getOrDefault(semesterId, Map.of()) : Map.of();
        Map<Long, Integer> after = new HashMap<>(before);
        Map<Long, Integer> offeredByRoot = new HashMap<>();
        Map<Long, Category> categories = snapshot.categories();
        if (semesterId != null && snapshot.validSemesters().contains(semesterId)) {
            for (var award : event.points().entrySet()) {
                Category category = categories.get(award.getKey());
                if (category == null || !category.active() || award.getValue() <= 0) {
                    continue;
                }
                after.merge(category.id(), award.getValue(), Integer::sum);
                Category root = propagate(category, categories, after);
                offeredByRoot.merge(root.id(), award.getValue(), Integer::sum);
            }
        }
        var gains = new ArrayList<Gain>();
        offeredByRoot.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Category root = categories.get(entry.getKey());
            int oldTotal = Math.min(root.maximum(), before.getOrDefault(root.id(), 0));
            int newTotal = Math.min(root.maximum(), after.getOrDefault(root.id(), 0));
            gains.add(new Gain(root.id(), root.name(), entry.getValue(), Math.max(0, newTotal - oldTotal)));
        });
        int useful = gains.stream().mapToInt(Gain::effectiveGain).sum();
        return new Benefits(useful, advances, parentBonus, fanpageMatch, typeMatches, gains);
    }

    private boolean completesParent(Map<Long, Criterion> criteria, Set<Long> completed, Criterion child) {
        Long parentId = child.parentId();
        Criterion parent = parentId == null ? null : criteria.get(parentId);
        if (parent == null || !parent.active() || completed.contains(parentId)) {
            return false;
        }
        long count = criteria.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().parentId(), parentId))
                .filter(entry -> completed.contains(entry.getKey())).count();
        return count + 1 == parent.requiredChildren();
    }

    private Category propagate(Category category, Map<Long, Category> categories, Map<Long, Integer> totals) {
        var visited = new HashSet<Long>();
        Category current = category;
        while (current.parentId() != null && visited.add(current.id())) {
            Category parent = categories.get(current.parentId());
            if (parent == null) {
                break;
            }
            var childTotals = categories.values().stream()
                    .filter(child -> Objects.equals(child.parentId(), parent.id()))
                    .mapToInt(child -> totals.getOrDefault(child.id(), 0));
            totals.put(parent.id(), parent.maxChild() ? childTotals.max().orElse(0) : childTotals.sum());
            current = parent;
        }
        return current;
    }

    public Ranked rank(Candidate candidate, Instant now) {
        boolean closed = !now.isBefore(candidate.deadline());
        boolean full = candidate.remainingSlots() == 0;
        int group = closed || full ? 2 : candidate.viewed() ? 1 : 0;
        double hours = Duration.between(now, candidate.deadline()).toMillis() / 3_600_000.0;
        double urgency = group == 2 ? 0
                : candidate.score().benefits().academic() * 0.10 * Math.clamp(1 - hours / 72, 0, 1);
        double effective = candidate.score().personalized() + urgency - candidate.readPenalty();
        return new Ranked(candidate, group, closed, full, urgency, effective);
    }
}
