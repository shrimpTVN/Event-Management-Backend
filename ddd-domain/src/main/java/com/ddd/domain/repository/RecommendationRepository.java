package com.ddd.domain.repository;

import com.ddd.domain.model.recommendation.RecommendationFeatures.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecommendationRepository {
    boolean needsRefresh(long studentId, LocalDate day, Instant now, Instant dayStart);
    Optional<Snapshot> lockSnapshot(long studentId);
    Snapshot loadSnapshot(long studentId, LocalDate day);
    void saveSnapshot(long studentId, Snapshot snapshot);
    List<EventFeatures> findStale(long studentId, LocalDate day, Instant now, Instant dayStart);
    void saveScore(long studentId, LocalDate day, EventFeatures event, Score score);
    List<Candidate> findCandidates(long studentId, Instant now, Instant dayStart);
}
