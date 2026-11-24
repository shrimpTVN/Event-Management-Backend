package com.ddd.application.service.event.impl;

import com.ddd.domain.model.port.EventRecommendationAiPort;
import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.RecommendationFeatures.*;
import com.ddd.domain.model.recommendation.RecommendationScoringPolicy;
import com.ddd.domain.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DailyRecommendationTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 23);
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");
    private static final Instant DAY_START = Instant.parse("2026-11-22T17:00:00Z");

    private final RecommendationScoringPolicy policy = new RecommendationScoringPolicy();

    @Test
    void awardsParentCompletionButIgnoresPointsUnderHigherSibling() {
        var benefits = policy.benefits(snapshot(DAY), event(21, 3, 5));

        assertEquals(0, benefits.usefulPoints());
        assertTrue(benefits.completesParent());
        assertEquals(50, benefits.academic());
        assertEquals(5, benefits.interest());
        assertEquals(4, policy.benefits(snapshot(DAY), event(21, 3, 14)).usefulPoints());
    }

    @Test
    void ordersAvailableUnseenBeforeViewedAndUnavailable() {
        var benefits = policy.benefits(snapshot(DAY), event(21, 3, 5));
        var strong = new Score(benefits, 4, "");
        var weak = new Score(new Benefits(0, false, false, false, 0, List.of()), 0, "");
        var unseen = new Candidate(3, NOW.plusSeconds(60), NOW, 1, 0, 0, false, weak);
        var viewed = new Candidate(2, NOW.plusSeconds(60), NOW, 1, 0, 5, true, strong);
        var full = new Candidate(1, NOW.plusSeconds(60), NOW, 0, 1, 0, false, strong);
        var closed = new Candidate(4, NOW, NOW, 1, 0, 0, false, strong);

        var ranks = List.of(full, viewed, unseen, closed).stream().map(e -> policy.rank(e, NOW))
                .sorted(RecommendationScoringPolicy.ORDER).toList();

        assertEquals(List.of(3L, 2L, 4L, 1L), ranks.stream().map(Ranked::id).toList());
        assertTrue(ranks.get(2).registrationClosed());
        assertTrue(ranks.get(3).full());
        assertEquals(0, ranks.get(3).urgencyScore());
        var reopened = new Candidate(1, full.deadline(), NOW, 1, 0, 0, false, strong);
        assertEquals(0, policy.rank(reopened, NOW).group());
        assertTrue(policy.rank(reopened, NOW).urgencyScore() > 0);
    }

    @Test
    void warmRequestUsesStoredSnapshotAndSkipsGemini() {
        var repository = mock(RecommendationRepository.class);
        var ai = mock(EventRecommendationAiPort.class);
        when(repository.needsRefresh(5, DAY, NOW, DAY_START)).thenReturn(false);

        new RecommendationRefreshService(repository, ai).prepare(5, NOW);

        verify(repository, never()).lockSnapshot(anyLong());
        verify(repository, never()).loadSnapshot(anyLong(), any());
        verify(repository, never()).saveScore(anyLong(), any(), any(), any());
        verifyNoInteractions(ai);
    }

    @Test
    void rollsOverAtVietnamMidnightAndFallsBackPerEvent() {
        var repository = mock(RecommendationRepository.class);
        var ai = mock(EventRecommendationAiPort.class);
        var next = DAY.plusDays(1);
        var midnight = DAY_START.plusSeconds(86_400);
        var snapshot = snapshot(next);
        var first = event(21, 3, 5);
        var second = event(22, 3, 5);

        when(repository.lockSnapshot(5)).thenReturn(Optional.of(snapshot(DAY)));
        when(repository.needsRefresh(5, next, midnight, midnight)).thenReturn(true);
        when(repository.loadSnapshot(5, next)).thenReturn(snapshot);
        when(repository.findStale(5, next, midnight, midnight)).thenReturn(List.of(first, second));
        when(ai.scoreDailyCandidates(anyList())).thenReturn(List.of(
                new AiRecommendationResult(21L, 50, "Valid"),
                new AiRecommendationResult(22L, 200, "Invalid")));

        new RecommendationRefreshService(repository, ai).prepare(5, midnight);

        verify(repository).saveSnapshot(5, snapshot);
        verify(repository).saveScore(eq(5L), eq(next), eq(first),
                argThat(score -> score.aiContribution() == 2 && score.explanation().equals("Valid")));
        verify(repository).saveScore(eq(5L), eq(next), eq(second),
                argThat(score -> Math.abs(score.aiContribution() - 10.0 / 3) < 0.0001));
    }

    private Snapshot snapshot(LocalDate day) {
        return new Snapshot(day, Map.of(10L, Map.of(101L, 10, 102L, 0, 103L, 10)),
                Map.of(20L, Set.of(31L)), Map.of(
                101L, new Category(101, null, "Parent", true, 20, true, DAY),
                102L, new Category(102, 101L, "Leaf", false, 20, true, DAY),
                103L, new Category(103, 101L, "Sibling", false, 20, true, DAY)),
                Map.of(30L, new Criterion(null, 2, true), 31L, new Criterion(30L, 1, true),
                        3L, new Criterion(30L, 1, true)),
                Set.of(1L), Map.of(2L, 2), Set.of(10L), Set.of(20L));
    }

    private EventFeatures event(long id, long criterion, int points) {
        return new EventFeatures(id, 1, "Event", "", 10L, 20L, criterion,
                "Criterion", 1L, 2L, Map.of(102L, points));
    }
}
