package com.ddd.domain.model.port;

import com.ddd.domain.model.recommendation.AiRecommendationResult;
import com.ddd.domain.model.recommendation.CandidateEventRecommendation;
import com.ddd.domain.model.recommendation.StudentRecommendationContext;

import java.util.List;

public interface EventRecommendationAiPort {
    List<AiRecommendationResult> generateExplanations(
            StudentRecommendationContext context,
            List<CandidateEventRecommendation> candidates
    );
}

