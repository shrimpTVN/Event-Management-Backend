package com.ddd.domain.model.recommendation;

import com.ddd.domain.model.recommendation.RecommendationFeatures.Benefits;

public record RecommendationAiCandidate(long eventId, String title, String description,
                                        Long semesterId, Long academicYearId, String criterionName,
                                        Benefits benefits) {}
