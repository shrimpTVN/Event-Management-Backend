package com.ddd.infrastructure.repository;

import com.ddd.domain.model.recommendation.RecommendationFeatures.*;
import com.ddd.domain.repository.RecommendationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RecommendationRepositoryImpl implements RecommendationRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    private static final String ELIGIBLE = """
            FROM events e
            JOIN event_recommendation_summary f ON f.event_id = e.id
            LEFT JOIN student_event_recommendation r ON r.event_id = e.id AND r.student_id = :student
            WHERE e.is_active AND e.status = 'PUBLISHED'
              AND e.date_happen >= :dayStart
              AND (e.date_open IS NULL OR e.date_open <= :now)
              AND e.date_close IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1 FROM registrations held
                  WHERE held.event_id = e.id AND held.user_id = :student AND held.status <> 'CANCELLED'
              )
            """;

    @Override
    public Optional<Snapshot> lockSnapshot(long studentId) {
        jdbc.sql("""
                INSERT INTO student_recommendation_summary(student_id) VALUES (:student)
                ON CONFLICT DO NOTHING
                """).param("student", studentId).update();
        return jdbc.sql("""
                SELECT COALESCE(snapshot::text, '') FROM student_recommendation_summary
                WHERE student_id = :student FOR UPDATE
                """).param("student", studentId).query(String.class).optional()
                .filter(json -> !json.isEmpty()).map(json -> decode(json, Snapshot.class));
    }

    @Override
    public Snapshot loadSnapshot(long studentId, LocalDate day) {
        String json = jdbc.sql("""
                SELECT jsonb_build_object(
                  'day', CAST(:day AS date),
                  'pointsBySemester', COALESCE((
                    SELECT jsonb_object_agg(semester_id, points) FROM (
                      SELECT sp.semester_id, jsonb_object_agg(sp.point_category_id, LEAST(pc.maximum, sp.total_point)) AS points
                      FROM student_semester_points sp JOIN point_categories pc ON pc.id = sp.point_category_id
                      WHERE sp.user_id = :student GROUP BY sp.semester_id
                    ) p
                  ), '{}'::jsonb),
                  'completedByYear', COALESCE((
                    SELECT jsonb_object_agg(academic_year_id, completed) FROM (
                      SELECT academic_year_id, jsonb_agg(criteria_id) AS completed
                      FROM student_criteria_progress
                      WHERE user_id = :student AND is_active AND is_completed GROUP BY academic_year_id
                    ) c
                  ), '{}'::jsonb),
                  'categories', COALESCE((
                    SELECT jsonb_object_agg(id, jsonb_build_object(
                      'id', id, 'parentId', parent_category_id, 'name', name,
                      'maxChild', is_max_child, 'maximum', maximum, 'active', is_active,
                      'dateApply', date_apply
                    )) FROM point_categories
                  ), '{}'::jsonb),
                  'criteria', COALESCE((
                    SELECT jsonb_object_agg(id, jsonb_build_object(
                      'parentId', parent_id, 'requiredChildren', COALESCE(required_children, 1),
                      'active', is_active
                    )) FROM criterias
                  ), '{}'::jsonb),
                  'fanpages', COALESCE((
                    SELECT jsonb_agg(fanpage_id) FROM (
                      SELECT fanpage_id FROM followings
                       WHERE user_id = :student AND is_active AND fol_status = 'FOLLOWING'
                      UNION SELECT fanpage_id FROM fanpage_members WHERE user_id = :student AND is_active
                    ) pages
                  ), '[]'::jsonb),
                  'typeCounts', COALESCE((
                    SELECT jsonb_object_agg(event_type_id, registrations) FROM (
                      SELECT history.event_type_id, COUNT(DISTINCT reg.event_id) AS registrations
                      FROM registrations reg JOIN events history ON history.id = reg.event_id
                      WHERE reg.user_id = :student AND reg.status NOT IN ('CANCELLED','ABSENT')
                        AND history.event_type_id IS NOT NULL GROUP BY history.event_type_id
                    ) affinity
                  ), '{}'::jsonb),
                  'validSemesters', COALESCE((SELECT jsonb_agg(id) FROM semesters WHERE is_active), '[]'::jsonb),
                  'validYears', COALESCE((SELECT jsonb_agg(id) FROM academic_years WHERE is_active), '[]'::jsonb)
                )::text
                """).param("student", studentId).param("day", day).query(String.class).single();
        return decode(json, Snapshot.class);
    }

    @Override
    public void saveSnapshot(long studentId, Snapshot snapshot) {
        jdbc.sql("""
                UPDATE student_recommendation_summary
                SET snapshot = CAST(:snapshot AS jsonb) WHERE student_id = :student
                """).param("student", studentId).param("snapshot", encode(snapshot)).update();
    }

    @Override
    public List<EventFeatures> findStale(long studentId, LocalDate day, Instant now, Instant dayStart) {
        return eligible("""
                SELECT (f.features || jsonb_build_object('id', e.id, 'version', f.scoring_version))::text
                """ + ELIGIBLE + """
                AND (r.score IS NULL OR r.score_day IS DISTINCT FROM :day
                     OR r.evaluated_event_version IS DISTINCT FROM f.scoring_version)
                ORDER BY e.id
                """, studentId, now, dayStart)
                .param("day", day).query(String.class)
                .list().stream().map(json -> decode(json, EventFeatures.class)).toList();
    }

    @Override
    public void saveScore(long studentId, LocalDate day, EventFeatures event, Score score) {
        jdbc.sql("""
                INSERT INTO student_event_recommendation
                    (student_id, event_id, score_day, evaluated_event_version, score)
                VALUES (:student, :event, :day, :version, CAST(:score AS jsonb))
                ON CONFLICT (student_id, event_id) DO UPDATE SET
                    score_day = EXCLUDED.score_day,
                    evaluated_event_version = EXCLUDED.evaluated_event_version,
                    score = EXCLUDED.score
                """).param("student", studentId).param("event", event.id())
                .param("day", day).param("version", event.version()).param("score", encode(score)).update();
    }

    @Override
    public List<Candidate> findCandidates(long studentId, Instant now, Instant dayStart) {
        return eligible("""
                WITH registration_counts AS (
                    SELECT event_id, count(*) AS registered FROM registrations
                    WHERE status <> 'CANCELLED'
                      AND event_id IN (SELECT id FROM events WHERE is_active AND status = 'PUBLISHED'
                                      AND date_happen >= :dayStart)
                    GROUP BY event_id
                )
                SELECT e.id, e.date_close, e.date_happen, e.capacity,
                       (SELECT registered FROM registration_counts WHERE event_id = e.id) AS registered,
                       r.read_penalty, r.last_read_at IS NOT NULL AS viewed, r.score::text
                """ + ELIGIBLE + " AND r.score IS NOT NULL", studentId, now, dayStart)
                .query((rs, row) -> {
                    long registered = rs.getLong("registered");
                    int remaining = (int) Math.max(0, rs.getInt("capacity") - registered);
                    return new Candidate(rs.getLong("id"), rs.getTimestamp("date_close").toInstant(),
                            rs.getTimestamp("date_happen").toInstant(), remaining, registered,
                            rs.getInt("read_penalty"), rs.getBoolean("viewed"),
                            decode(rs.getString("score"), Score.class));
                }).list();
    }

    @Override
    public boolean needsRefresh(long studentId, LocalDate day, Instant now, Instant dayStart) {
        return eligible("""
                SELECT NOT EXISTS (
                    SELECT 1 FROM student_recommendation_summary
                    WHERE student_id = :student AND (snapshot->>'day')::date >= :day
                ) OR EXISTS (SELECT 1
                """ + ELIGIBLE + """
                    AND (r.score IS NULL OR r.score_day IS DISTINCT FROM (
                        SELECT (snapshot->>'day')::date FROM student_recommendation_summary WHERE student_id = :student
                    ) OR r.evaluated_event_version IS DISTINCT FROM f.scoring_version)
                )
                """, studentId, now, dayStart).param("day", day).query(Boolean.class).single();
    }

    private JdbcClient.StatementSpec eligible(String sql, long studentId, Instant now, Instant dayStart) {
        return jdbc.sql(sql).param("student", studentId)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .param("dayStart", OffsetDateTime.ofInstant(dayStart, ZoneOffset.UTC));
    }

    private String encode(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot encode recommendation data", exception);
        }
    }

    private <T> T decode(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot decode recommendation data", exception);
        }
    }
}
