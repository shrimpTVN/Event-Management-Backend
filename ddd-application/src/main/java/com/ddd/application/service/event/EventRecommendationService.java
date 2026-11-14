package com.ddd.application.service.event;

import com.ddd.application.dto.recommendation.EventRecommendationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRecommendationService {
    Page<EventRecommendationDto> getRecommendationsForStudent(String email, Pageable pageable);
}

