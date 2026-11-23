package com.ddd.application.service.event.impl;

import com.ddd.domain.model.port.EventRecommendationAiPort;
import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.RecommendationAiCandidate;
import com.ddd.domain.model.recommendation.RecommendationFeatures.*;
import com.ddd.domain.model.recommendation.RecommendationScoringPolicy;
import com.ddd.domain.repository.RecommendationRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationRefreshService {
    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int BATCH_SIZE = 10;
    private final RecommendationRepository repository;
    private final EventRecommendationAiPort ai;
    private final RecommendationScoringPolicy policy = new RecommendationScoringPolicy();

    @Transactional
    public void prepare(long studentId, Instant now) {
        var today = now.atZone(VIETNAM).toLocalDate();
        var dayStart = today.atStartOfDay(VIETNAM).toInstant();
        if (!repository.needsRefresh(studentId, today, now, dayStart)) {
            return;
        }
        Snapshot snapshot = repository.lockSnapshot(studentId).orElse(null);
        if (snapshot == null || snapshot.day().isBefore(today)) {
            snapshot = repository.loadSnapshot(studentId, today);
            repository.saveSnapshot(studentId, snapshot);
        }
        List<EventFeatures> stale = repository.findStale(studentId, snapshot.day(), now, dayStart);
        for (int from = 0; from < stale.size(); from += BATCH_SIZE) {
            scoreBatch(studentId, snapshot, stale.subList(from, Math.min(from + BATCH_SIZE, stale.size())));
        }
    }

    private void scoreBatch(long studentId, Snapshot snapshot, List<EventFeatures> events) {
        var candidates = new ArrayList<RecommendationAiCandidate>(events.size());
        var benefitsById = new HashMap<Long, Benefits>();
        for (EventFeatures event : events) {
            Benefits benefits = policy.benefits(snapshot, event);
            benefitsById.put(event.id(), benefits);
            String description = event.description();
            if (description != null && description.length() > 2000) {
                description = description.substring(0, 2000);
            }
            candidates.add(new RecommendationAiCandidate(event.id(), event.title(), description,
                    event.semesterId(), event.yearId(), event.criterionName(), benefits));
        }
        var results = new HashMap<Long, AiRecommendationResult>();
        for (AiRecommendationResult result : ai.scoreDailyCandidates(candidates)) {
            if (benefitsById.containsKey(result.eventId()) && Double.isFinite(result.aiScore())
                    && result.aiScore() >= 0 && result.aiScore() <= 100) {
                results.putIfAbsent(result.eventId(), result);
            }
        }
        for (EventFeatures event : events) {
            Benefits benefits = benefitsById.get(event.id());
            AiRecommendationResult result = results.get(event.id());
            double aiContribution = result == null ? benefits.interest() * (4.0 / 6.0) : result.aiScore() * 0.04;
            String explanation = result == null ? explain(benefits) : result.aiExplanation();
            repository.saveScore(studentId, snapshot.day(), event, new Score(benefits, aiContribution, explanation));
        }
    }

    private String explain(Benefits benefits) {
        var reasons = new ArrayList<String>();
        if (benefits.usefulPoints() > 0) {
            reasons.add("Có thể bổ sung " + benefits.usefulPoints() + " điểm rèn luyện");
        }
        if (benefits.completesParent()) {
            reasons.add("có thể hoàn thành tiêu chí cấp trên");
        } else if (benefits.fulfillsCriteria()) {
            reasons.add("có thể hoàn thành một tiêu chí");
        }
        if (benefits.fanpageMatch()) {
            reasons.add("đến từ trang bạn theo dõi hoặc tham gia");
        }
        if (benefits.typeMatches() > 0) {
            reasons.add("cùng loại với sự kiện bạn đã đăng ký");
        }
        return reasons.isEmpty() ? "Sự kiện gợi ý để bạn khám phá." : String.join(", ", reasons) + ".";
    }
}
