package com.ddd.domain.model.recommendation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecommendationFeatures {
    private RecommendationFeatures() {}

    public record EventFeatures(long id, long version, String title, String description,
                                Long semesterId, Long yearId, Long criterionId, String criterionName,
                                Long fanpageId, Long typeId, Map<Long, Integer> points) {}

    public record Category(long id, Long parentId, String name, boolean maxChild, int maximum,
                           boolean active, LocalDate dateApply) {}

    public record Criterion(Long parentId, int requiredChildren, boolean active) {}

    public record Snapshot(LocalDate day, Map<Long, Map<Long, Integer>> pointsBySemester,
                           Map<Long, Set<Long>> completedByYear, Map<Long, Category> categories,
                           Map<Long, Criterion> criteria, Set<Long> fanpages, Map<Long, Integer> typeCounts,
                           Set<Long> validSemesters, Set<Long> validYears) {}

    public record Gain(Long categoryId, String categoryName, int pointsOffered, int effectiveGain) {}

    public record Benefits(int usefulPoints, boolean fulfillsCriteria, boolean completesParent,
                           boolean fanpageMatch, int typeMatches, List<Gain> pointGains) {
        public double academic() {
            int criteriaPoints = completesParent ? 50 : fulfillsCriteria ? 25 : 0;
            return Math.min(50.0, usefulPoints * 10.0) + criteriaPoints;
        }

        public double interest() {
            return (fanpageMatch ? 3 : 0) + Math.min(3, typeMatches);
        }
    }

    public record Score(Benefits benefits, double aiContribution, String explanation) {
        public double personalized() {
            return benefits.academic() + benefits.interest() + aiContribution;
        }
    }

    public record Candidate(long id, Instant deadline, Instant happensAt, int remainingSlots,
                            long registeredCount, int readPenalty, boolean viewed, Score score) {}

    public record Ranked(Candidate candidate, int group, boolean registrationClosed, boolean full,
                         double urgencyScore, double effectiveScore) {
        public long id() {
            return candidate.id();
        }
    }
}
