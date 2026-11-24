package com.ddd.infrastructure.repository;

import com.ddd.domain.model.recommendation.EventViewBatch;
import com.ddd.domain.repository.EventViewRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventViewRepositoryImpl implements EventViewRepository {
    private final JdbcClient jdbc;

    @Override
    public void record(long studentId, EventViewBatch batch, Instant receivedAt) {
        if (batch.eventIds().isEmpty()) {
            return;
        }
        jdbc.sql("""
                WITH accepted AS (
                    INSERT INTO recommendation_view_receipt (student_id, event_id, batch_id)
                    SELECT :studentId, id, :batchId
                    FROM events
                    WHERE id IN (:eventIds) AND is_active AND status = 'PUBLISHED'
                    ORDER BY id
                    ON CONFLICT (student_id, event_id, batch_id) DO NOTHING
                    RETURNING student_id, event_id
                )
                INSERT INTO student_event_recommendation AS recommendation
                    (student_id, event_id, read_penalty, last_read_at)
                SELECT student_id, event_id, 5, :receivedAt
                FROM accepted
                ORDER BY event_id
                ON CONFLICT (student_id, event_id) DO UPDATE SET
                    read_penalty = LEAST(20, recommendation.read_penalty + EXCLUDED.read_penalty),
                    last_read_at = GREATEST(recommendation.last_read_at, EXCLUDED.last_read_at)
                """)
                .param("studentId", studentId)
                .param("batchId", batch.batchId())
                .param("eventIds", batch.eventIds())
                .param("receivedAt", OffsetDateTime.ofInstant(receivedAt, ZoneOffset.UTC))
                .update();
    }

    @Override
    public Map<Long, Integer> findPenalties(long studentId) {
        return jdbc.sql("""
                SELECT event_id, read_penalty
                FROM student_event_recommendation
                WHERE student_id = :studentId AND last_read_at IS NOT NULL
                """)
                .param("studentId", studentId)
                .query((rs, row) -> Map.entry(rs.getLong("event_id"), rs.getInt("read_penalty")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
