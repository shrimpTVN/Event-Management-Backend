package com.ddd.infrastructure.adapter.ai;

import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.CandidateEventRecommendation;
import com.ddd.domain.model.recommendation.StudentRecommendationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeminiEventRecommendationAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return empty list when candidate list is empty")
    void generateExplanations_whenCandidatesEmpty_returnsEmptyList() {
        GeminiEventRecommendationAdapter adapter = new GeminiEventRecommendationAdapter(
                "", "gemini-1.5-flash", objectMapper
        );

        StudentRecommendationContext context = new StudentRecommendationContext(1L, "sv@test.com", Map.of(), Set.of());
        List<AiRecommendationResult> results = adapter.generateExplanations(context, Collections.emptyList());

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should return deterministic fallback explanations when API key is blank")
    void generateExplanations_whenApiKeyBlank_returnsFallbackExplanation() {
        GeminiEventRecommendationAdapter adapter = new GeminiEventRecommendationAdapter(
                "", "gemini-1.5-flash", objectMapper
        );

        StudentRecommendationContext context = new StudentRecommendationContext(
                1L, "sv@test.com", Map.of(1L, 10), Set.of(2L)
        );

        CandidateEventRecommendation candidate = new CandidateEventRecommendation(
                101L,
                "Hội thảo Công nghệ 2026",
                "Mô tả hội thảo",
                15, // remaining slots
                2,  // days until happen
                10, // total effective points
                true,
                "Học tập tốt",
                85.0
        );

        List<AiRecommendationResult> results = adapter.generateExplanations(context, List.of(candidate));

        assertNotNull(results);
        assertEquals(1, results.size());
        AiRecommendationResult res = results.get(0);
        assertEquals(101L, res.eventId());
        assertEquals(85.0, res.aiScore());
        assertNotNull(res.aiExplanation());
        assertTrue(res.aiExplanation().contains("10 điểm rèn luyện"));
        assertTrue(res.aiExplanation().contains("Học tập tốt"));
        assertTrue(res.aiExplanation().contains("2 ngày tới"));
        assertTrue(res.aiExplanation().contains("15 slot"));
    }

    @Test
    @DisplayName("Should support context with named criteria and point categories")
    void generateExplanations_withNamedContext_returnsFallbackExplanation() {
        GeminiEventRecommendationAdapter adapter = new GeminiEventRecommendationAdapter(
                "", "gemini-1.5-flash", objectMapper
        );

        StudentRecommendationContext context = new StudentRecommendationContext(
                1L,
                "sv@test.com",
                Map.of(1L, 10),
                Set.of(2L),
                Map.of("Ý thức tham gia học tập", 10),
                List.of("Tình nguyện tốt")
        );

        CandidateEventRecommendation candidate = new CandidateEventRecommendation(
                202L,
                "Chiến dịch Tiếp sức Mùa thi",
                "Mô tả chiến dịch",
                5,
                1,
                15,
                true,
                "Tình nguyện tốt",
                90.0,
                true,
                60.0
        );

        List<AiRecommendationResult> results = adapter.generateExplanations(context, List.of(candidate));

        assertNotNull(results);
        assertEquals(1, results.size());
        AiRecommendationResult res = results.get(0);
        assertEquals(202L, res.eventId());
        assertTrue(res.aiExplanation().contains("15 điểm rèn luyện"));
        assertTrue(res.aiExplanation().contains("Tình nguyện tốt"));
        assertTrue(res.aiExplanation().contains("CLB/Đơn vị bạn đang theo dõi"));
    }
}

